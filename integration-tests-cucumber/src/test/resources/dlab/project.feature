Feature: Project management in DLab
  Such feature allowed to manage projects inside DLab

  Scenario Outline: Create new project when it does not exist

    Given There is no project with name "<name>" in DLab
    And User try to create new project with name "<name>", endpoints "<endpoints>", groups "<groups>" and key "<key>"
    When User send create new project request
    Then User wait maximum "<timeout>" minutes while project is creating
    Then Status code is 200
    Examples:
      | name | endpoints | groups   | key              | timeout |
      | prj1 | test      | $anyyser | publicKeyContent | 10      |