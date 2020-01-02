@project
Feature: Project management in DLab
  Such feature allowed to manage projects inside DLab

  Scenario Outline: Create new project when it does not exist and use shared image is enable

    Given There is no project with name "<name>" in DLab
    And There are the following endpoints
      | local |
    And There are the following groups
      | $anyuser |
    And User generates new publicKey
    And User tries to create new project with name "<name>", endpoints, groups, publicKey and use shared image enable "true"
    When User sends create new project request
    Then User waits maximum <timeout> minutes while project is creating
    Then Status code is 200
    Examples:
      | name | timeout |
      | prj1 | 20      |


  Scenario Outline: Create new project when project with the same name exists already

    Given There are the following projects
      | prj1 |
    And There are the following endpoints
      | local |
    And There are the following groups
      | $anyuser |
    And User generates new publicKey
    And User tries to create new project with name "<name>", endpoints, groups, publicKey and use shared image enable "true"
    When User sends create new project request
    Then Status code is 409
    Examples:
      | name |
      | prj1 |


  Scenario Outline: Get information about project that exits

    Given There are the following projects
      | prj1 |
    And There are the following endpoints
      | local |
    And There are the following groups
      | $anyuser |
    And User tries to get information about project with name "<name>"
    When User sends request to get information about project
    Then Status code is 200
    And Project information is successfully returned with name "<name>", endpoints, groups
    Examples:
      | name |
      | prj1 |


  Scenario: Get information about a project that does not exists

    Given There is no project with name "test1" in DLab
    And User tries to get information about project with name "test1"
    When User sends request to get information about project
    Then Status code is 404


  Scenario: Edit (change use shared image) a project that is available

    Given There is a project with name "prj1" in DLab
    And User tries to edit project with shared image enable opposite to existing
    When User sends edit request
    Then Status code is 200
    And Project information is successfully updated with shared image enable

  @terminate
  Scenario Outline: Terminate a project/edge node that exits

    Given There is a project with name "<name>" in DLab
    And User tries to terminate the project with name "<name>"
    When User sends termination request
    Then User waits maximum <timeout> minutes while project is terminated
    Then Status code is 200
    Examples:
      | name | timeout |
      | prj1 | 20      |


  @terminate
  Scenario: Terminate a project/edge node that does not exists

    Given There is no project with name "test1" in DLab
    And User tries to terminate the project with name "test1"
    When User sends termination request
    Then Status code is 404
