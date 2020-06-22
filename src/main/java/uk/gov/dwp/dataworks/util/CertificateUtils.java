package uk.gov.dwp.dataworks.util;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import kotlin.Pair;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.dwp.dataworks.errors.FetchCrlException;
import uk.gov.dwp.dataworks.errors.RevokedClientCertificateException;
import uk.gov.dwp.dataworks.logging.DataworksLogger;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.*;

@Component
public class CertificateUtils {

    public CertificateUtils(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }

    public synchronized void checkCertificatesAgainstCrl(Certificate[] certificates) {
        if (certificates != null) {
                crlCache.entrySet().stream()
                        .map(Map.Entry::getValue)
                        .map(AcmPcaCrl::getCrl)
                        .forEach(crl -> asList(certificates).stream()
                                .map(X509Certificate.class::cast)
                                .forEach(certificate -> checkRevocation(crl, certificate)));
        }
    }

    public void checkRevocation(X509CRL crl, X509Certificate certificate) {
        X509CRLEntry revocationEntry = crl.getRevokedCertificate(certificate.getSerialNumber());
        String serialNumber = certificate.getSerialNumber().toString();
        if (revocationEntry != null) {
            LOGGER.error("Client attempted to use service with revoked certificate",
                    new Pair<>("certificate_serial_number", serialNumber),
                    new Pair<>("revocation_reason", revocationEntry.getRevocationReason().toString()),
                    new Pair<>("revocation_date", revocationEntry.getRevocationDate().toString()),
                    new Pair<>("serial_number_from_certificate", serialNumber),
                    new Pair<>("subject_principal", certificate.getSubjectX500Principal().getName()),
                    new Pair<>("issuer_dn", certificate.getIssuerDN().getName()));
            throw new RevokedClientCertificateException(serialNumber);
        }
        else {
            LOGGER.info("Valid certificate used by client", new Pair<>("certificate_serial_number", serialNumber.toString()));
        }
    }

    @Scheduled(initialDelay = 1000, fixedRateString = "${crl.check.interval:300000}")
    public void refreshCrls() {
        try {
            String crlBucket = StringUtils.isNotBlank(this.crlBucket) ? this.crlBucket : String.format("dw-%s-crl", environmentName);
            LOGGER.info("Getting crl bucket objects",
                    new Pair<>("crl_bucket", crlBucket),
                    new Pair<>("crl_prefix", crlCommonPrefix));

            ListObjectsV2Request request = new ListObjectsV2Request().withBucketName(crlBucket).withPrefix(crlCommonPrefix);
            ListObjectsV2Result results = amazonS3.listObjectsV2(request);
            List<S3ObjectSummary> summaries = results.getObjectSummaries();
            Set<String> crlsInS3 = summaries.stream().map(x -> x.getKey())
                    .filter(key -> key.endsWith(".crl"))
                    .collect(Collectors.toSet());
            Set<String> removals = crlCache.keySet().stream().filter(k -> !crlsInS3.contains(k)).collect(Collectors.toSet());

            synchronized (this) {

                removals.stream().forEach(key -> {
                    LOGGER.info("Removing crl from cache, no longer on s3",
                            new Pair<>("crl_key", key),
                            new Pair<>("s3_crls", crlsInS3.toString()));
                    crlCache.remove(key);
                });

                summaries.stream().forEach(summary -> {
                    try {
                        String key = summary.getKey();
                        if (key.endsWith(".crl")) {
                            if (crlCache.containsKey(key)) {
                                String previousEtag = crlCache.get(key).getEtag();
                                String latestEtag = summary.getETag();
                                if (!previousEtag.equals(latestEtag)) {
                                    LOGGER.info("Replacing cached crl", new Pair<>("crl_key", key),
                                            new Pair<>("previous_etag", previousEtag), new Pair<>("latest_etag", latestEtag));

                                    X509CRL crl =  crl(amazonS3.getObject(crlBucket, key).getObjectContent());
                                    crlCache.put(summary.getKey(), new AcmPcaCrl(summary.getETag(),crl));
                                }
                            }
                            else {
                                LOGGER.info("Adding new crl to cache", new Pair<>("crl_key", key));
                                X509CRL crl =  crl(amazonS3.getObject(crlBucket, key).getObjectContent());
                                crlCache.put(summary.getKey(), new AcmPcaCrl(summary.getETag(), crl));
                            }
                        }
                    }
                    catch (FetchCrlException e) {
                        LOGGER.error("Failed to fetch crl", e);
                    }
                });
            }
        }
        catch (SdkClientException e) {
            LOGGER.warn("Failed to refresh crls", new Pair<>("exception_message", e.getMessage()));
        }
    }

    public X509CRL crl(InputStream inputStream) throws FetchCrlException {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X509");
            return (X509CRL) cf.generateCRL(inputStream);
        }
        catch (CertificateException | CRLException e) {
            throw new FetchCrlException(e);
        }
        finally {
            try {
                inputStream.close();
            }
            catch (IOException e) {
                LOGGER.warn("Failed to close crl stream", new Pair<>("exception_message", e.getMessage()));
            }
        }
    }

    @Value("${server.environment_name}")
    private String environmentName;

    @Value("${crl.bucket:}")
    private String crlBucket;

    @Value("${crl.common.prefix:crl}")
    private String crlCommonPrefix;


    private final AmazonS3 amazonS3;

    private Map<String, AcmPcaCrl> crlCache = new HashMap<>();
    private final static DataworksLogger LOGGER = DataworksLogger.Companion.getLogger(CertificateUtils.class.toString());
}
