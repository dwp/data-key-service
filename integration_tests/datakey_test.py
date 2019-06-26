from assertpy import assert_that
import requests


def get_new_data_key():
    response = requests.get("http://localhost:8080/datakey")
    response.raise_for_status()
    return response.json()


def decrypt_data_key(key_id, encrypted_key):
    response = requests.post("http://localhost:8080/datakey/actions/decrypt?keyId=" + key_id, data=encrypted_key)
    response.raise_for_status()
    return response.json()


def test_get_ping():
    response = requests.get("http://localhost:8080/ping")
    response.raise_for_status()


def test_health_check():
    response = requests.get("http://localhost:8080/healthcheck")
    response.raise_for_status()
    body = response.json()
    assert_that(body.keys()).contains_only('encryptionService', 'masterKey',
                                           'dataKeyGenerator', 'encryption', 'decryption')
    values = body.values()
    assert_that(values).contains_only('OK')


def test_data_key_returns_valid_data_key_pair():
    # Given a new data_key is generated
    data_key = get_new_data_key()

    # When the data_key is decrypted
    decrypted_key = decrypt_data_key(
        data_key.get("dataKeyEncryptionKeyId"),
        data_key.get("ciphertextDataKey"))

    # Then the data_key has all the required values
    assert_that(data_key.keys()).contains_only("dataKeyEncryptionKeyId", "plaintextDataKey", "ciphertextDataKey")

    # And the decrypted key matches the plaintext key
    assert_that(decrypted_key.get("plaintextDataKey")).is_equal_to(data_key.get("plaintextDataKey"))
