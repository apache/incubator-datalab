/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.datalab.properties;

public interface ChangePropertiesConst {
    String GKE_SELF_SERVICE_PATH = "/root/self-service.yaml";
    String GKE_SELF_SERVICE = "self-service.yaml";
    String SELF_SERVICE = "self-service.yml";
    String SELF_SERVICE_PROP_PATH = "/opt/datalab/conf/self-service.yml";
    //    String SELF_SERVICE_PROP_PATH = "services/self-service/self-service.yml";
    String PROVISIONING_SERVICE = "provisioning.yml";
    String PROVISIONING_SERVICE_PROP_PATH = "/opt/datalab/conf/provisioning.yml";
//String PROVISIONING_SERVICE_PROP_PATH = "services/provisioning-service/provisioning.yml";

    String BILLING_SERVICE = "billing.yml";
    String BILLING_SERVICE_PROP_PATH = "/opt/datalab/conf/billing.yml";
    //            String BILLING_SERVICE_PROP_PATH = "services/billing-gcp/billing.yml";
    //      String BILLING_SERVICE_PROP_PATH = "services/billing-azure/billing.yml";
//    String BILLING_SERVICE_PROP_PATH = "services/billing-aws/billing.yml";
    String GKE_BILLING_PATH = "/root/billing.yaml";
    String GKE_BILLING_SERVICE = "billing.yml";
    String RESTART_URL = "config/restart";
    String LOCAL_ENDPOINT_NAME = "local";
    String BASE_CONFIG_URL = "config";

    String SELF_SERVICE_SUPERVISORCTL_RUN_NAME = " ui ";
    String PROVISIONING_SERVICE_SUPERVISORCTL_RUN_NAME = " provserv ";
    String BILLING_SERVICE_SUPERVISORCTL_RUN_NAME = " billing ";
    String SECRET_REGEX = "([sS]ecret|[pP]assword|accessKeyId|secretAccessKey):\\s?(.*)";
    String USER_REGEX = "(user|username):\\s?(.*)";
    String SECRET_REPLACEMENT_FORMAT = " ***********";
    String SUPERVISORCTL_RESTART_SH_COMMAND = "sudo supervisorctl restart";
    String CHANGE_CHMOD_SH_COMMAND_FORMAT = "sudo chmod %s %s";
    String DEFAULT_CHMOD = "644";
    String WRITE_CHMOD = "777";

    String LICENCE_REGEX = "# \\*{50,}";
    String LICENCE =
            "# *****************************************************************************\n" +
                    "#\n" +
                    "# Licensed to the Apache Software Foundation (ASF) under one\n" +
                    "# or more contributor license agreements. See the NOTICE file\n" +
                    "# distributed with this work for additional information\n" +
                    "# regarding copyright ownership. The ASF licenses this file\n" +
                    "# to you under the Apache License, Version 2.0 (the\n" +
                    "# \"License\"); you may not use this file except in compliance\n" +
                    "# with the License. You may obtain a copy of the License at\n" +
                    "#\n" +
                    "# http://www.apache.org/licenses/LICENSE-2.0\n" +
                    "#\n" +
                    "# Unless required by applicable law or agreed to in writing,\n" +
                    "# software distributed under the License is distributed on an\n" +
                    "# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
                    "# KIND, either express or implied. See the License for the\n" +
                    "# specific language governing permissions and limitations\n" +
                    "# under the License.\n" +
                    "#\n" +
                    "# ******************************************************************************";

    int DEFAULT_VALUE_PLACE = 1;
    int DEFAULT_NAME_PLACE = 0;
}
