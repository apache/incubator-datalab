#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
Feature: Endpoint management in DataLab
  Such feature allowed to manage endpoint inside DataLab

  Scenario Outline: Create new endpoint when it does not exist

    Given There is no endpoints in DataLab
    And User try to create new endpoint with name "<name>" and uri "<uri>" and account "<account>" and "<tag>"
    When User send create new endpoint request
    Then Response status code is 200
    And Endpoint URI is present in location header
    Examples:
      | name          | uri     | account   | tag      |
      | test_endpoint | someuri | 123231312 | some_tag |


  Scenario Outline: Create new endpoint when it exist already

    Given There is no endpoints in DataLab
    And User try to create new endpoint with name "<name>" and uri "<uri>" and account "<account>" and "<tag>"
    And  User send create new endpoint request
    When User try to create new endpoint with name "<name>" and uri "<uri>" and account "<account>" and "<tag>"
    And User send create new endpoint request
    Then Response status code is 409
    Examples:
      | name          | uri     | account   | tag      |
      | test_endpoint | someuri | 123231312 | some_tag |


  Scenario Outline: Get information for endpoint

    Given There is no endpoints in DataLab
    And User try to create new endpoint with name "<name>" and uri "<uri>" and account "<account>" and "<tag>"
    And  User send create new endpoint request
    When User try to get information about endpoint with name "<name>"
    Then Response status code is 200
    And Endpoint information is successfully returned with name "<name>", uri "<uri>", account "<account>", and tag "<tag>"
    Examples:
      | name          | uri     | account   | tag      |
      | test_endpoint | someuri | 123231312 | some_tag |


  Scenario: Get list of endpoints

    Given There is no endpoints in DataLab
    And User try to create new endpoint with name "test1" and uri "someuri1" and account "123" and "customTag1"
    And  User send create new endpoint request
    And User try to create new endpoint with name "test2" and uri "someuri2" and account "1233" and "customTag4"
    And  User send create new endpoint request
    When User try to get information about endpoints
    Then Response status code is 200
    And There are endpoints with name test1 and test2

  Scenario: Get not endpoint that does not exist

    Given There is no endpoints in DataLab
    When User try to get information about endpoint with name "someName"
    Then Response status code is 404
