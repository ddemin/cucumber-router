# language: en
@EnvTest2
@Priority1
Feature: Some feature for Test2 with priority 1

  Scenario: Scenario1 (feature 3 with priority 1)
    Given Step 1
    When Step 2
    Then Step 3

  @tagB
  Scenario: Scenario2 (feature 3 with priority 1)
    Given Step 1
    When Step 2
    Then Step 3