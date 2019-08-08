Feature: DLab login API
  Used to check DLab login flow

  Scenario Outline: User try to login to DLab
    Given User try to login to Dlab with "<username>" and "<password>"
    When user try to login
    Then response code is "<status>"

    Examples:
      | username       | password | status |
      | test           | pass     | 200    |
      | not_valid_user | pass     | 401    |