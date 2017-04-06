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
package com.epam.dlab.rest.contracts;

public class ApiCallbacks {
    public static final String API = "/api";
    public static final String KEY_LOADER = API + "/user/access_key/callback";
    public static final String COMPUTATIONAL = API + "/infrastructure_provision/computational_resources";
    public static final String EXPLORATORY = API + "/infrastructure_provision/exploratory_environment";
    public static final String INFRASTRUCTURE = API + "/infrastructure";
    public static final String EDGE = INFRASTRUCTURE + "/edge";
    public static final String STATUS_URI = "/status";
}
