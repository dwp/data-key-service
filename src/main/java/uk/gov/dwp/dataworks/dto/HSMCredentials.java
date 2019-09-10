package uk.gov.dwp.dataworks.dto;

import java.util.Objects;

public class HSMCredentials {

    private String userName;

    private String passWord;
    private String clusterId;
    public HSMCredentials(String userName, String passWord, String clusterId) {
        this.userName = userName;
        this.passWord = passWord;
        this.clusterId = clusterId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HSMCredentials that = (HSMCredentials) o;
        return userName.equals(that.userName) &&
                passWord.equals(that.passWord) &&
                clusterId.equals(that.clusterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, passWord, clusterId);
    }

    public String getUserName() {
        return userName;
    }

    public String getPassWord() {
        return passWord;
    }

    public String getClusterId() {
        return clusterId;
    }
}
