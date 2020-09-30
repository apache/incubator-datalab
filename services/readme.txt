/***************************************************************************

Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.

****************************************************************************/

How to run locally the self service and provisioning service on development mode.


1.	Install Mongo database
1.1 Download MongoDB from https://www.mongodb.com/download-center
1.2. Install database follow manual
1.3. Run server and create accounts
> mongo
use admin
db.createUser(
   {
     user: "admin",
     pwd: "<password>",
     roles: [ { role: "dbAdminAnyDatabase", db: "admin" },
              { role: "userAdminAnyDatabase", db: "admin" },
              { role: "readWriteAnyDatabase", db: "admin" } ]
   }
)

use datalabdb
db.createUser(
   {
     user: "admin",
     pwd: "<password>",
     roles: [ "dbAdmin", "userAdmin", "readWrite" ]
   }
)
1.4. Load collections
mongoimport -u admin -p <password> -d datalabdb -c settings mongo_settings.json

2.	Setting up environment options
2.1. Set configuration file ..\..\infrastructure-provisioning\src\ssn\templates\ssn.yml
# DEV_MODE="true"
2.2. Add system environment variable
DATALAB_CONF_DIR=...\infrastructure-provisioning\src\ssn\templates
or create two symlinks to service\provisioning-service and service\self-service for
..\..\infrastructure-provisioning\src\ssn\templates\ssn.yml
Unix
  ln -s ssn.yml ../../infrastructure-provisioning/src/ssn/templates/ssn.yml
Windows
  mklink ssn.yml ..\..\infrastructure-provisioning\src\ssn\templates\ssn.yml
2.3 For Unix create two folders:
  /var/opt/datalab/log/ssn
  /opt/datalab/tmp/result

3.	Install Node.js
3.1. Install Node.js from https://nodejs.org/en/
3.2. Add Node.js installation folder to environment variable PATH
3.3. Install packages
	npm install npm@latest -g
4. Change folder to \datalab\services\self-service\src\main\resources\webapp and install
	npm i
5. Buid web application:
	npm run build.prod

4.	Run application
4.1. Run provisioning-service
4.2. Run self-service
4.3. Try access to http://localhost:8080
     User: test
     Password: <any>
