# language: en
@EnvUnknownTest1
Feature: Some feature for unknown env with no priority

  @EnvTest1
  Scenario: Scenario1 (feature 7 with no priority)
    Given Step 1
    When Step 2
    Then Step 3

  @EnvTest1
  Scenario: Scenario2 (feature 7 with no priority)
    Given Step 1
    When Step 2
    Then Step 3