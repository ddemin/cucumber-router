# language: en
@EnvTest2
Feature: Some feature for Test2 with no priority

  @tagB @tagA
  Scenario: Scenario1 (feature 4 with no priority)
    Given Step 1
    When Step 2
    Then Step 3

  @tagB @tagD
  Scenario: Scenario2 (feature 4 with no priority)
    Given Step 1
    When Step 2
    Then Step 3