from assertpy import assert_that
import requests


def test_get_new_data_key():
    response = requests.get("http://localhost:8080/datakey")
    response.raise_for_status()
    return response.json()


def test_get_ping():
    response = requests.get("http://localhost:8080/ping")
    response.raise_for_status()
    return response.json()


def test_health_check():
    response = requests.get("http://localhost:8080/healthcheck")
    response.raise_for_status()
    assert_that(test_health_check()).contains_only({'encryptionService': 'OK', 'masterKey': 'OK',
                                                   'dataKeyGenerator': 'OK', 'encryption': 'OK', 'decryption': 'OK'})
    return response.json()


def test_decrypt_data_key(key_id, encrypted_key):
    response = requests.request(
        "POST",
        "http://localhost:8080/datakey/decrypt",
        params={"keyId": key_id},
        body=encrypted_key)
    response.raise_for_status()
    return response.text()


def test_data_key_returns_valid_data_key_pair():
    # Given a new data_key is generated
    data_key = test_get_new_data_key()

    # When the data_key is decrypted
    decrypted_key = test_decrypt_data_key(
        data_key.get("dataKeyEncryptionKeyId"),
        data_key.get("ciphertextDataKey"))

    # Then the data_key has all the required values
    assert_that(data_key.keys()).contains_only("dataKeyEncryptionKeyId", "plaintextDataKey", "ciphertextDataKey")

    # And the decrypted key matches the plaintext key
    assert_that(decrypted_key).is_equal_to(data_key.get("plaintextDataKey"))




