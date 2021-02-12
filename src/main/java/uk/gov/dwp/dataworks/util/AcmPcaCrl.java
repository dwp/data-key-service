package uk.gov.dwp.dataworks.util;

import java.security.cert.X509CRL;
import java.util.Objects;

public class AcmPcaCrl {

    public AcmPcaCrl(String etag, X509CRL crl)  {
        this.etag = etag;
        this.crl = crl;
    }

    public String getEtag() {
        return etag;
    }

    public X509CRL getCrl() {
        return crl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AcmPcaCrl acmPcaCrl = (AcmPcaCrl) o;
        return etag.equals(acmPcaCrl.etag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(etag);
    }

    private final String etag;
    private final X509CRL crl;
}
