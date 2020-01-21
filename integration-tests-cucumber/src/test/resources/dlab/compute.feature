Feature: Computational resource management in DLab
    Such feature allowed to to manage computational resource inside DLab

    Scenario Outline: Create new data engine when it does not exist

      Given There is active project "<project>"
      And There is running notebook "<notebook>"
      And There is no data engine with name "<name>"
      And User tries to create new data engine with name "<name>", count "<count>", shape "<shape>", notebook "<notebook>", image "<image>", template "<template>", config "<config>", project "<project>", exploratory tag "<exploratoryTag>"
      When User sends create new data engine request
      Then Status code is 200 for data engine
      And User waits maximum <timeout> minutes while data engine is creating
      Examples:
        | name | count | shape     | notebook | image                  | template                        | config | project | exploratoryTag       | timeout |
        | sp1  | 2     | c4.xlarge | jup1     | docker.dlab-dataengine | Apache Spark standalone cluster |        | prj1    | integration test tag | 10      |



    Scenario Outline: Create new data engine service when it does not exist

      Given There is active project "<project>"
      And There is running notebook "<notebook>"
      And There is no data engine service with name "<name>"
      And User tries to create new data engine service with name "<name>", count "<count>", master shape "<masterShape>", slave shape "<slaveShape>", version "<version>", notebook "<notebook>", image "<image>", template "<template>", spot "<spot>", config "<config>", project "<project>", exploratory tag "<exploratoryTag>"
      When User sends create new data engine service request
      Then Status code is 200 for data engine service
      And User waits maximum <timeout> minutes while data engine is creating
      Examples:
      | name | count | masterShape | slaveShape | version    | notebook | image                          | template        | spot  |  config | project | exploratoryTag       | timeout |
      | des1 | 2     | c4.xlarge   | c4.xlarge  | emr-5.28.0 | zep1     | docker.dlab-dataengine-service | AWS EMR cluster | false |         | prj1    | integration test tag | 30      |



    Scenario Outline: Stop data engine when it is in running Status

      Given There is running data engine with name "<name>" on notebook "<notebook>" in project "<project>"
      And User tries to stop the data engine
      When User sends request to stop the data engine
      Then Status code is 202
      And User waits maximum <timeout> minutes while data engine is stopping
      Examples:
        | name | notebook | project | timeout |
        | sp1  | jup1     | prj1    | 7       |


    Scenario Outline: Start data engine when it is in stopped Status

      Given There is stopped data engine with name "<name>" on notebook "<notebook>" in project "<project>"
      And User tries to start the data engine
      When User sends request to start the data engine
      Then Status code is 202
      And User waits maximum <timeout> minutes while data engine is starting
      Examples:
      | name | notebook | project | timeout |
      | sp1  | jup1     | prj1    | 7       |


    Scenario Outline: Create new data engine when it already exists with the same name

      Given There is active project "<project>"
      And There is running notebook "<notebook>"
      And There is data engine with name "<name>"
      And User tries to create new data engine with name "<name>", count "<count>", shape "<shape>", notebook "<notebook>", image "<image>", template "<template>", config "<config>", project "<project>", exploratory tag "<exploratoryTag>"
      When User sends create new data engine request
      Then Status code is 409 for data engine
      Examples:
      | name | count | shape     | notebook | image                  | template                        | config | project | exploratoryTag       |
      | sp1  | 2     | c4.xlarge | jup1     | docker.dlab-dataengine | Apache Spark standalone cluster |        | prj1    | integration test tag |



    Scenario Outline: Create new data engine service when it already exists with the same name

      Given There is active project "<project>"
      And There is running notebook "<notebook>"
      And There is data engine service with name "<name>"
      And User tries to create new data engine service with name "<name>", count "<count>", master shape "<masterShape>", slave shape "<slaveShape>", version "<version>", notebook "<notebook>", image "<image>", template "<template>", spot "<spot>", config "<config>", project "<project>", exploratory tag "<exploratoryTag>"
      When User sends create new data engine service request
      Then Status code is 409 for data engine service
      Examples:
      | name | count | masterShape | slaveShape | version    | notebook | image                          | template        | spot  |  config | project | exploratoryTag       |
      | des1 | 2     | c4.xlarge   | c4.xlarge  | emr-5.28.0 | zep1     | docker.dlab-dataengine-service | AWS EMR cluster | false |         | prj1    | integration test tag |



    Scenario Outline: Get information about data engine that exits

      Given There is running data endine
      | sp1 |
      And There is active project
      | prj1 |
      And There is running notebooks
      | jup1|
      And User tries to get information about data engine with name "<name>" on notebook "<notebook>" in project "<project>"
      When User sends request to get information about data engine
      Then Status code is 200
      Examples:
      | name | notebook | project |
      | sp1  | jup1     | prj1    |



      Scenario Outline: Get information about data engine service that exits

        Given There is running data endine service
        | des1 |
        And There is active project
        | prj1 |
        And There is running notebooks
        | zep1 |
        And User tries to get information about data engine service with name "<name>" on notebook "<notebook>" in project "<project>"
        When User sends request to get information about data engine service
        Then Status code is 200
        Examples:
        | name  | notebook | project |
        | des1  | zep1     | prj1    |



    Scenario Outline: Terminate data engine when it is in running Status

        Given There is running data engine with name "<name>" on notebook "<notebook>" in project "<project>"
        And User tries to terminate the data engine
        When User sends request to terminate the data engine
        Then Status code is 202
        And User waits maximum <timeout> minutes while data engine is terminating
        Examples:
        | name | notebook | project | timeout |
        | sp1  | jup1     | prj1    | 7       |



    Scenario Outline: Terminate data engine service when it is in running Status

        Given There is running data engine service with name "<name>" on notebook "<notebook>" in project "<project>"
        And User tries to terminate the data engine service
        When User sends request to terminate the data engine service
        Then Status code is 202
        And User waits maximum <timeout> minutes while data engine service is terminating
        Examples:
        | name | notebook | project | timeout |
        | des1  | jup1     | prj1    | 7       |
