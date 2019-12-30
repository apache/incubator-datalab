Feature: Project management in DLab
  Such feature allowed to manage projects inside DLab

  Scenario Outline: Create new project when it does not exist and use shared image is enable

    Given There is no project with name "<name>" in DLab
    And There are the following endpoints
      | local |
    And There are the following groups
      | $anyuser |
    And User generates new publicKey
    When User tries to create new project with name "<name>", endpoints, groups, publicKey and use shared image "enable"
    And User sends create new project request
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
    When User tries to create new project with name "<name>", endpoints, groups, publicKey and use shared image "enable"
    And User sends create new project request
    Then Response status code is 409
    Examples:
      | name |
      | prj1 |


  Scenario Outline: Get information about projects that exits

    Given There are the following projects
      | prj1 |
    And There are the following endpoints
      | local |
    And There are the following groups
      | $anyuser |
    When User tries to get information about project with name "<name>"
    Then Response status code is 200
    And Project information is successfully returned with name "<name>", endpoints, groups
    Examples:
      | name | endpoint | group |
      | prj1 | local    | $anyuser |


  Scenario: Get information about a project that does not exists

    Given There is no project with name "test1"
    When User tries to get information about the project with name "test1"
    Then Respone status code is 404




   Scenario: Edit (change use shared image and add a group) a project that is available

   Given There are the following projects
     | prj1 |
   And There are the following endpoints
     | local |
   And There are the following groups
     | $anyuser |
   And Use shared image
     | enable |
   When User tries to create a new group with name "admin"
   And User adds new group with name "admin"
   And User changes use shared image to "false"
   And User sends request for updating
   Then Response status code is 200
   And Project information is successfully updated with groups, use shared image


   Scenario: Edit (remove endpoint) a project that is available

   Given There are the following projects
     | prj1 |
   And There are the following endpoints
     | local |
   And There are the following groups
     | $anyuser |
   And Use shared image
     | enable |
   When User tries to remove endpoint with name "local"
   And User sends request for updating
   Then Response status code is 403

   Scenario: Edit (add group that does not exit) a project that is available

   Given There are the following projects
     | prj1 |
   And There are the following endpoints
     | local |
   And There are the following groups
     | $anyuser |
   And Use shared image
     | enable |
   And There is no group with name "global"
   When User tries to add group with name "global"
   And User sends request for updating
   Then Response status code is 400

   Scenario: Edit (add endpoint that does not exit) a project that is available

   Given There are the following projects
     | prj1 |
   And There are the following endpoints
     | local |
   And There are the following groups
     | $anyuser |
   And Use shared image
     | enable |
   And There is no endpoint with name "exploring"
   When User tries to add endpoint with name "exploring"
   And User sends request for updating
   Then Response status code is 400

   Scenario Outline: Terminate a project/edge node  that exits

     Given There are the following projects
       | prj1 |
     And There are the following endpoints
       | local |
     And There are the following groups
       | $anyuser  |
     When User tries to terminate the project with name "<name>"
     And User sends request for termination
     Then User waits maximum <timeout> minutes while project is terminated
     Then Response status code is 200
     Examples:
       | name | timeout |
       | prj1 | 20      |


    Scenario: Terminate a project/edge node that does not exists

      Given There is no project with name "test1"
      When User tries to terminate the project with name "test1"
      Then Respone status code is 404
