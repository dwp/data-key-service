import time

import requests
from assertpy import assert_that
from behave import given, step, then

DKS_CERTIFICATE = "dks-crt.pem"
TESTS_KEY = "integration-tests-key.pem"
TESTS_CERTIFICATE = "integration-tests-crt.pem"


@given("dks is up and healthy")
def step_impl(context):
    response = requests.get("https://dks:8443/healthcheck",
                            cert=(TESTS_CERTIFICATE,
                                  TESTS_KEY),
                            verify=DKS_CERTIFICATE).json()
    assert_that(response).contains_key('encryptionService', 'masterKey', 'dataKeyGenerator', 'encryption', 'decryption',
                                       'correlationId', 'trustedCertificates')
    status_items = [item for item in response.items() if
                    item[0] != 'correlationId' and item[0] != 'trustedCertificates']
    for k, v in status_items:
        assert_that(v).is_equal_to('OK')


@step("A datakey has been acquired")
def step_impl(context):
    response = requests.get("https://dks:8443/datakey?correlationId=integration_tests",
                            cert=(TESTS_CERTIFICATE,
                                  TESTS_KEY),
                            verify=DKS_CERTIFICATE).json()
    assert_that(response).contains_key('dataKeyEncryptionKeyId', 'plaintextDataKey', 'ciphertextDataKey',
                                       'correlationId')
    context.datakey_response = response


@step("The datakey has been decrypted")
def step_impl(context):
    decrypt_response = requests.post(
        f"https://dks:8443/datakey/actions/decrypt?keyId={context.datakey_response['dataKeyEncryptionKeyId']}&correlationId=integration_tests",
        data=context.datakey_response['ciphertextDataKey'],
        cert=(TESTS_CERTIFICATE,
              TESTS_KEY),
        verify=DKS_CERTIFICATE).json()

    assert_that(decrypt_response).contains_key('dataKeyDecryptionKeyId', 'plaintextDataKey', 'correlationId')


@then("the metrics should be available on prometheus")
def step_impl(context):
    time.sleep(2)  # give time for the scrape
    response = requests.get("http://prometheus:9090/api/v1/targets/metadata").json()
    assert_that(response).contains_key('data')
    data = response['data']
    metrics_names = (x['metric'] for x in data)
    name_list = list(metrics_names)
    name_list.sort()
    assert_that(name_list).contains('dks_unhealthy_check', 'http_server_requests_seconds', 'logback_events_total')


@step("{query} counter should equal {expected_value}")
def step_impl(context, query: str, expected_value: str):
    url = f"http://prometheus:9090/api/v1/query?query=http_server_requests_seconds_count{{uri='{query}'}}"
    response = requests.get(url).json()
    result = response['data']['result']
    assert_that(result).is_length(1)
    assert_that(result[0]).contains_key('metric').contains_key('value')
    metric = result[0]['metric']
    assert_that(metric).contains_key('instance')
    assert_that(metric).contains_key('environment')
    assert_that(metric).contains_key('job')
    assert_that(metric['instance']).is_equal_to('dks:8443')
    assert_that(metric['environment']).is_equal_to('local')
    assert_that(metric['job']).is_equal_to('dks')
    value = result[0]['value']
    assert_that(value[1]).is_equal_to(expected_value)
