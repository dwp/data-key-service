from assertpy import assert_that
import requests


def get_new_datakey():
    response = requests.get("http://localhost:8080/datakey")
    response.raise_for_status()
    return response.json()


def decrypt_datakey(key_id, encrypted_key):
    response = requests.request(
        "POST",
        "http://localhost:8080/datakey/decrypt",
        params={"keyId": key_id},
        body=encrypted_key)
    response.raise_for_status()
    return response.text()


def datakey_returns_valid_datakey_pair():
    # Given a new datakey is generated
    data_key = get_new_datakey()

    # When the datakey is decrypted
    decrypted_key = decrypt_datakey(
        data_key.get("dataKeyEncryptionKeyId"),
        data_key.get("ciphertextDataKey"))

    # Then the datakey has all the required values
    assert_that(data_key.keys()).contains_only("dataKeyEncryptionKeyId", "plaintextDataKey", "ciphertextDataKey")

    # And the decrypted key matches the plaintext key
    assert_that(decrypted_key).is_equal_to(data_key.get("plaintextDataKey"))


