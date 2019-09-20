package uk.gov.dwp.dataworks.dto;

import java.util.Objects;

public class HSMCredentials {

    private final String userName;
    private final String passWord;
    private final String partitionId;
    public HSMCredentials(String userName, String passWord, String partitionId) {
        this.userName = userName;
        this.passWord = passWord;
        this.partitionId = partitionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HSMCredentials that = (HSMCredentials) o;
        return userName.equals(that.userName) &&
                passWord.equals(that.passWord) &&
                partitionId.equals(that.partitionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, passWord, partitionId);
    }

    public String getUserName() {
        return userName;
    }

    public String getPassWord() {
        return passWord;
    }

    public String getPartitionId() {
        return partitionId;
    }
}
