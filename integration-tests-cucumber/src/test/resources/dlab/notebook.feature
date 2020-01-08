@notebook
Feature: Notebook management in DLab
  Such feature allowed to manage notebook inside DLab

  Scenario Outline: Create new notebook when it does not exist

    Given There is active project "<project>" in DLab
    And There is no notebook with name "<name>"
    And User tries to create new notebook with name "<name>", endpoint "<endpoint>", image "<image>", template "<template>", project "<project>", exploratory tag "<exploratoryTag>", shape "<shape>", version "<version>", image name "<imageName>"
    When User sends create new notebook request
    Then Status code is 200 for notebook
    And User waits maximum <timeout> minutes while notebook is creating
    @aws @v1 @jupyter
    Examples:
      | name | endpoint | image               | template               | project | exploratoryTag       | shape     | version                | imageName | timeout |
      | jup1 | local    | docker.dlab-jupyter | Jupyter notebook 6.0.2 | prj1    | integration test tag | t2.medium | jupyter_notebook-6.0.2 |           | 20      |