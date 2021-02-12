package uk.gov.dwp.dataworks.util;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.dwp.dataworks.errors.FetchCrlException;
import uk.gov.dwp.dataworks.errors.RevokedClientCertificateException;

import java.math.BigInteger;
import java.security.cert.CRLReason;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class CertificateUtilsTest {

    @Test(expected = RevokedClientCertificateException.class)
    public void testThrowsOnRevoked() throws ParseException {
        AmazonS3 amazonS3 = Mockito.mock(AmazonS3.class);
        CertificateUtils utils = new CertificateUtils(amazonS3);
        X509CRL crl = mock(X509CRL.class);
        X509Certificate certificate = mock(X509Certificate.class);
        X509CRLEntry revocationEntry = mock(X509CRLEntry.class);
        given(revocationEntry.getRevocationDate()).willReturn(new SimpleDateFormat("yyyy-MM-dd").parse("2020-03-04"));
        given(revocationEntry.getRevocationReason()).willReturn(CRLReason.KEY_COMPROMISE);
        BigInteger serialNumber = new BigInteger("123");
        given(certificate.getSerialNumber()).willReturn(serialNumber);
        given(crl.getRevokedCertificate(serialNumber)).willReturn(revocationEntry);
        utils.checkRevocation(crl, certificate);
    }

    @Test
    public void testDoesNothingOnNotRevoked() {
        AmazonS3 amazonS3 = Mockito.mock(AmazonS3.class);
        CertificateUtils utils = new CertificateUtils(amazonS3);
        X509CRL crl = mock(X509CRL.class);
        X509Certificate certificate = mock(X509Certificate.class);
        BigInteger serialNumber = new BigInteger("123");
        given(certificate.getSerialNumber()).willReturn(serialNumber);
        given(crl.getRevokedCertificate(serialNumber)).willReturn(null);
        utils.checkRevocation(crl, certificate);
    }

    @Test
    public void testAddCrls() throws FetchCrlException {
        String crlBucket = "CRL_BUCKET";
        String crlCommonPrefix = "CRL_COMMON_PREFIX";
        AmazonS3 amazonS3 = Mockito.mock(AmazonS3.class);
        ListObjectsV2Result results = Mockito.mock(ListObjectsV2Result.class);
        given(amazonS3.listObjectsV2(any(ListObjectsV2Request.class))).willReturn(results);

        S3Object crlObject1 = mockS3Object();
        S3Object crlObject2 = mockS3Object();

        String crlObject1Key = "CRL_KEY_1.crl";
        String crlObject2Key = "CRL_KEY_2.crl";

        String crlObject1Etag = "CRL_1_ETAG";
        String crlObject2Etag = "CRL_2_ETAG";

        given(amazonS3.getObject(crlBucket, crlObject1Key)).willReturn(crlObject1);
        given(amazonS3.getObject(crlBucket, crlObject2Key)).willReturn(crlObject2);

        S3ObjectSummary crlSummary1 = mockS3ObjectSummary(crlObject1Key, crlObject1Etag);
        S3ObjectSummary crlSummary2 = mockS3ObjectSummary(crlObject2Key, crlObject2Etag);

        given(results.getObjectSummaries()).willReturn(Arrays.asList(crlSummary1, crlSummary2));

        CertificateUtils real = new CertificateUtils(amazonS3);
        Map<String, AcmPcaCrl> cache = mock(Map.class);

        ReflectionTestUtils.setField(real, "crlCache", cache);
        ReflectionTestUtils.setField(real, "crlBucket", crlBucket);
        ReflectionTestUtils.setField(real, "crlCommonPrefix", crlCommonPrefix);

        X509CRL crl1 = mock(X509CRL.class);
        X509CRL crl2 = mock(X509CRL.class);
        CertificateUtils utils = spy(real);
        doReturn(crl1).doReturn(crl2).when(utils).crl(any());

        utils.refreshCrls();

        verify(utils, times(2)).crl(any());
        ArgumentCaptor<String> prefixCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<AcmPcaCrl> acmPcaCrlArgumentCaptor = ArgumentCaptor.forClass(AcmPcaCrl.class);
        verify(cache, times(2)).put(prefixCaptor.capture(), acmPcaCrlArgumentCaptor.capture());
        List<String> keys = prefixCaptor.getAllValues();
        assertEquals(keys.get(0), crlObject1Key);
        assertEquals(keys.get(1), crlObject2Key);
        List<AcmPcaCrl> crls = acmPcaCrlArgumentCaptor.getAllValues();
        assertEquals(crls.get(0), new AcmPcaCrl(crlObject1Etag, crl1));
        assertEquals(crls.get(1), new AcmPcaCrl(crlObject2Etag, crl2));
    }

    @Test
    public void testRemovesCrls() throws FetchCrlException {
        String crlObject1Key = "CRL_KEY_1.crl";
        String crlObject2Key = "CRL_KEY_2.crl";
        String crlObject1EtagV1 = "CRL_1_ETAG_V1";
        String crlObject2EtagV1 = "CRL_2_ETAG_V1";
        X509CRL crl1 = mock(X509CRL.class);
        X509CRL crl2 = mock(X509CRL.class);

        Map<String, AcmPcaCrl> realCache = new HashMap<>();
        realCache.put(crlObject1Key, new AcmPcaCrl(crlObject1EtagV1, crl1));
        realCache.put(crlObject2Key, new AcmPcaCrl(crlObject2EtagV1, crl2));
        Map<String, AcmPcaCrl> cache = spy(realCache);

        AmazonS3 amazonS3 = Mockito.mock(AmazonS3.class);
        CertificateUtils real = new CertificateUtils(amazonS3);

        String crlBucket = "CRL_BUCKET";
        String crlCommonPrefix = "CRL_COMMON_PREFIX";

        ReflectionTestUtils.setField(real, "crlCache", cache);
        ReflectionTestUtils.setField(real, "crlBucket", crlBucket);
        ReflectionTestUtils.setField(real, "crlCommonPrefix", crlCommonPrefix);

        CertificateUtils utils = spy(real);

        ListObjectsV2Result results1 = Mockito.mock(ListObjectsV2Result.class);
        given(amazonS3.listObjectsV2(any(ListObjectsV2Request.class))).willReturn(results1);

        S3Object crlObject1 = mockS3Object();
        given(amazonS3.getObject(crlBucket, crlObject1Key)).willReturn(crlObject1);
        S3ObjectSummary crlSummary1 = mockS3ObjectSummary(crlObject1Key, crlObject1EtagV1);
        given(results1.getObjectSummaries()).willReturn(Collections.singletonList(crlSummary1));

        utils.refreshCrls();

        verify(utils, times(0)).crl(any());
        verify(cache, times(0)).put(any(), any());
        verify(cache, times(1)).remove(crlObject2Key);
    }

    @Test
    public void testReplacesCrls() throws FetchCrlException {
        String crlObjectKey = "CRL_KEY.crl";
        String crlObjectEtagV1 = "CRL_ETAG_V1";
        String crlObjectEtagV2 = "CRL_ETAG_V2";
        X509CRL crlv1 = mock(X509CRL.class);
        X509CRL crlv2 = mock(X509CRL.class);

        Map<String, AcmPcaCrl> realCache = new HashMap<>();
        realCache.put(crlObjectKey, new AcmPcaCrl(crlObjectEtagV1, crlv1));
        Map<String, AcmPcaCrl> cache = spy(realCache);

        AmazonS3 amazonS3 = Mockito.mock(AmazonS3.class);
        CertificateUtils real = new CertificateUtils(amazonS3);

        String crlBucket = "CRL_BUCKET";
        String crlCommonPrefix = "CRL_COMMON_PREFIX";
        ReflectionTestUtils.setField(real, "crlCache", cache);
        ReflectionTestUtils.setField(real, "crlBucket", crlBucket);
        ReflectionTestUtils.setField(real, "crlCommonPrefix", crlCommonPrefix);

        CertificateUtils utils = spy(real);
        doReturn(crlv2).when(utils).crl(any());

        ListObjectsV2Result results = Mockito.mock(ListObjectsV2Result.class);
        given(amazonS3.listObjectsV2(any(ListObjectsV2Request.class))).willReturn(results);

        S3Object crlObject = mockS3Object();
        given(amazonS3.getObject(crlBucket, crlObjectKey)).willReturn(crlObject);
        S3ObjectSummary crlSummary = mockS3ObjectSummary(crlObjectKey, crlObjectEtagV2);
        given(results.getObjectSummaries()).willReturn(Collections.singletonList(crlSummary));

        utils.refreshCrls();

        ArgumentCaptor<String> prefixCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<AcmPcaCrl> acmPcaCrlArgumentCaptor = ArgumentCaptor.forClass(AcmPcaCrl.class);

        verify(utils, times(1)).crl(any());
        verify(cache, times(1)).put(prefixCaptor.capture(), acmPcaCrlArgumentCaptor.capture());

        assertEquals(prefixCaptor.getValue(), crlObjectKey);
        assertEquals(acmPcaCrlArgumentCaptor.getValue(), new AcmPcaCrl(crlObjectEtagV2, crlv2));
    }

    private S3Object mockS3Object() {
        S3Object s3Object = mock(S3Object.class);
        S3ObjectInputStream objectInputStream = mock(S3ObjectInputStream.class);
        given(s3Object.getObjectContent()).willReturn(objectInputStream);
        return s3Object;
    }

    private S3ObjectSummary mockS3ObjectSummary(String key, String etag) {
        S3ObjectSummary crlSummary1 = mock(S3ObjectSummary.class);
        given(crlSummary1.getKey()).willReturn(key);
        given(crlSummary1.getETag()).willReturn(etag);
        return crlSummary1;
    }
}
