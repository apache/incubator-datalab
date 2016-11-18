/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.backendapi.dao;

import static com.epam.dlab.backendapi.dao.MongoSetting.*;
import static com.mongodb.client.model.Filters.eq;
import static org.apache.commons.lang3.StringUtils.EMPTY;

public class SettingsDAO extends BaseDAO {
    private static final String VALUE = "value";

    public String getServiceBaseName() {
        return getSetting(SERIVICE_BASE_NAME);
    }

    public String getAwsRegion() {
        return getSetting(AWS_REGION);
    }

    public String getSecurityGroups() {
        return getSetting(SECURITY_GROUPS);
    }

    public String getExploratorySshUser() {
        return getSetting(EXPLORATORY_SSH_USER);
    }

    public String getCredsKeyDir() {
        return getSetting(CREDS_KEY_DIRECTORY);
    }

    private String getSetting(MongoSetting setting) {
        return mongoService.getCollection(SETTINGS).find(eq(ID, setting.getId())).first().getOrDefault(VALUE, EMPTY).toString();
    }
}
