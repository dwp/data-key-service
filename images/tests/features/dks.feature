Feature: Makes metrics available
  Metrics are made available at a suitable end point and are
  successfully scraped

  Scenario: Exposes metrics
    Given dks is up and healthy
    Given A datakey has been acquired
    And The datakey has been decrypted
    Then the metrics should be available on prometheus
    And /datakey counter should equal 1
    And /datakey/actions/decrypt counter should equal 1
