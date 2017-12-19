# language: en
@EnvUnknownTest2
Feature: Some feature for unknown env with no priority

  @EnvTest2
  Scenario: Scenario1 (feature 7 with no priority)
    Given Step 1
    When Step 2
    Then Step 3

  @EnvTest2
  Scenario: Scenario2 (feature 7 with no priority)
    Given Step 1
    When Step 2
    Then Step 3