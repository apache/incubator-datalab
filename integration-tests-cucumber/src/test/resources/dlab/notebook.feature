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
    Examples:
      | name    | endpoint | image                      | template                      | project | exploratoryTag       | shape     | version                | imageName | timeout |
      | jup1    | local    | docker.dlab-jupyter        | Jupyter notebook 6.0.2        | prj1    | integration test tag | t2.medium | jupyter_notebook-6.0.2 |           | 50      |
      | rst1    | local    | docker.dlab-rstudio        | RStudio 1.2.5033              | prj1    | integration test tag | t2.medium | RStudio-1.2.5033       |           | 45      |
      | zep1    | local    | docker.dlab-zeppelin       | Apache Zeppelin 0.8.2         | prj1    | integration test tag | t2.medium | zeppelin-0.8.2         |           | 50      |
      | tenrst1 | local    | docker.dlab-tensor-rstudio | RStudio with TensorFlow 1.8.0 | prj1    | integration test tag | p2.xlarge | tensorflow_gpu-1.8.0   |           | 60      |
      | ten1    | local    | docker.dlab-tensor         | Jupyter with TensorFlow 1.8.0 | prj1    | integration test tag | p2.xlarge | tensorflow_gpu-1.8.0   |           | 60      |
      | juplab1 | loacl    | docker.dlab-jupyterlab     | JupyterLab 0.35.6             | prj1    | integration test tag | t2.medium | jupyter_lab-0.35.6     |           | 30      |
      | deepl1  | loacl    | docker.dlab-deeplearning   | Deep Learning  2.2            | prj1    | integration test tag | p2.xlarge | deeplearning-2.2       |           | 180     |



Scenario Outline: Create new notebook when it already exists with the same name

Given There is active project "<project>" in DLab
    And There is notebook with name "<name>"
    And User tries to create new notebook with name "<name>", endpoint "<endpoint>", image "<image>", template "<template>", project "<project>", exploratory tag "<exploratoryTag>", shape "<shape>", version "<version>", image name "<imageName>"
    When User sends create new notebook request
    Then Status code is 409 for notebook
    Examples:
      | name    | endpoint | image                      | template                      | project | exploratoryTag       | shape     | version                | imageName |
      | jup1    | local    | docker.dlab-jupyter        | Jupyter notebook 6.0.2        | prj1    | integration test tag | t2.medium | jupyter_notebook-6.0.2 |           | 
      | rst1    | local    | docker.dlab-rstudio        | RStudio 1.2.5033              | prj1    | integration test tag | t2.medium | RStudio-1.2.5033       |           | 
      | zep1    | local    | docker.dlab-zeppelin       | Apache Zeppelin 0.8.2         | prj1    | integration test tag | t2.medium | zeppelin-0.8.2         |           | 
      | tenrst1 | local    | docker.dlab-tensor-rstudio | RStudio with TensorFlow 1.8.0 | prj1    | integration test tag | p2.xlarge | tensorflow_gpu-1.8.0   |           |
      | ten1    | local    | docker.dlab-tensor         | Jupyter with TensorFlow 1.8.0 | prj1    | integration test tag | p2.xlarge | tensorflow_gpu-1.8.0   |           | 
      | juplab1 | loacl    | docker.dlab-jupyterlab     | JupyterLab 0.35.6             | prj1    | integration test tag | t2.medium | jupyter_lab-0.35.6     |           | 
      | deepl1  | loacl    | docker.dlab-deeplearning   | Deep Learning  2.2            | prj1    | integration test tag | p2.xlarge | deeplearning-2.2       |           | 


  Scenario Outline: Stop notebook when it is in running Status

    Given There is running notebook with name "<name>" in project "<project>"
    And User tries to stop the notebook
    When User sends request to stop the notebook
    Then Status code is 202
    And User waits maximum <timeout> minutes while notebook is stopping
    Examples:
      | name | project | timeout |
      | jup1 | prj1    | 10      |



  Scenario Outline: Start notebook when it is in stopped Status

        Given There is stopped notebook with name "<name>" in project "<project>"
        And User tries to start the notebook
        When User sends request to start the notebook
        Then Status code is 202
        And User waits maximum <timeout> minutes while notebook is starting
        Examples:
          | name | project | timeout |
          | jup1 | prj1    | 10      |



  Scenario Outline: Get information about notebook that exits

        Given There are the following notebook
            | jup1 |
        And There are the following project
            | prj1 |
        And User tries to get information about notebook with name "<name>"
        When User sends request to get information about notebook
        Then Status code is 200
        And Notebook information is successfully returned with name "<name>", project "<project>"
        Examples:
        | name  | project |
        | jup1  | prj1    |



  Scenario Outline: Get information about notebooks that exit

        Given There are the following notebooks
            | jup1 | rst1  | zep1 | tenrst1 | ten1 | juplab1 | deepl1  |
        And There are the following project
            | prj1 |
        And User tries to get information about notebook with name "<name>" in project "<project>"
        When User sends request to get information about notebook
        Then Status code is 200
        And Notebook information is successfully returned with name "<name>", project "<project>"
        Examples:
        | name   | project |
        | jup1   | prj1    |
        | rst1   | prj1    |
        | zep1   | prj1    |
        | tenrst | prj1    |
        | jup1   | prj1    |
        | jup1   | prj1    |


    Scenario Outline: Get information about a notebook that does not exists

        Given There is no notebook with name "<name>" in project "<project>" in DLab
        And User tries to get information about notebook with name "<name>"
        When User sends request to get information about notebook
        Then Status code is 404
        Examples:
        | name  | project |
        | test1 | prj1    |



      Scenario Outline: Terminate notebook when it is in running Status

        Given There is running notebook with name "<name>" in project "<project>"
        And User tries to terminate the notebook
        When User sends request to terminate the notebook
        Then Status code is 202
        And User waits maximum <timeout> minutes while notebook is terminating
        Examples:
        | name | project | timeout |
        | jup1 | prj1    | 10      |
