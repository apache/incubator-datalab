### Prerequisites for DataLab post-deployment

- Service account with following roles:
```
Compute Admin
Compute Network Admin
Dataproc Administrator
Role Administrator
Service Account Admin
Service Account User
Project IAM Admin
Storage Admin 
BigQuery Data Viewer
BigQuery Job User
```
- Google Cloud Storage JSON API should be enabled
- Keycloak server with specific client for DataLab UI (could be dpeloyed with Kecylaok deployment script)

Service account should be created manually and attached to the instance with post-deployment script.

### Executing post-deployment script

To configure SSN node, following steps should be executed:

- Connect to the instance via SSH and run the following commands:
```
/usr/bin/python /opt/datalab/sources/infrastructure-provisioning/scripts/post-deployment_configuration.py
    --keycloak_realm_name <value>
    --keycloak_auth_server_url <value>
    --keycloak_client_name <value>
    --keycloak_client_secret <value>
    --keycloak_user <value>
    --keycloak_admin_password <value>
```

List of parameters for SSN node post-deployment script:
| Parameter                     | Description/Value                                                                   |
|-------------------------------|-------------------------------------------------------------------------------------|
| keycloak\_realm\_name         | Keycloak realm name                                                                 |
| keycloak\_auth\_server\_url   | Url of Keycloak auth server                                                         |
| keycloak\_client\_name        | Name of client for DataLab UI                                                          |
| keycloak\_client\_secret      | Secret of client for DataLab UI                                                        |
| kkeycloak\_user               | Keycloak user with administrator permissions                                        |
| keycloak\_admin\_password     | Password for Keycloak user with administrator permissions                           |