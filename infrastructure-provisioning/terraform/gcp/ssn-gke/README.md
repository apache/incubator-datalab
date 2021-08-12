# DataLab GKE Deployment <a name="DataLab_Deployment"></a>

### Preparing environment for DataLab deployment <a name="Env_for_DataLab"></a>

Prerequisites:

- IAM user
- Service account and JSON auth file for it. In order to get JSON auth file, Key should be created for service account 
through Google cloud console.
- Google Cloud Storage JSON API should be enabled

Preparation steps for deployment:

- Create an VM instance with the following settings:
    - The instance should have access to Internet in order to install required prerequisites
    - Boot disk OS Image - Ubuntu 18.04
- Put JSON auth file created through Google cloud console to users home directory
- Install Terraform v0.12.3:
```
sudo su
apt-get update
wget https://releases.hashicorp.com/terraform/0.12.3/terraform_0.12.3_linux_amd64.zip
apt-get install unzip
unzip terraform_0.12.3_linux_amd64.zip
chmod +x terraform
mv terraform /usr/local/bin/
```
- Install jq and kubectl:
```
apt-get install jq
snap install kubectl --classic
```
- Install Python3.7 and pip libraries:
```
add-apt-repository ppa:deadsnakes/ppa
apt-get install python3.7
update-alternatives --install /usr/bin/python3 python3 /usr/bin/python3.8 1
update-alternatives --install /usr/bin/python3 python3 /usr/bin/python3.7 2
update-alternatives --config python3
apt-get install python3-pip
pip3 install fabric==2.4.0 patchwork==1.0.1 invoke==1.2.0 cryptography==3.3.1
```
- Install Git and clone DataLab repository:
```
git clone https://github.com/apache/incubator-datalab.git
```

### Executing deployment scripts

Deployment of DataLab starts from GKE cluster creating.

To build GKE cluster with DataLab, following steps should be executed:

- Connect to the instance via SSH and run the following commands:
```
mkdir /home/ubuntu/datalab-state
cd incubator-datalab/infrastructure-provisioning/terraform/bin/
git checkout develop
```
- Run python script to create GKE cluster and deploy Helm Charts:
```
python3 datalab.py deploy gcp k8s --credentials_file_path /path/to/auth/file.json --project_id gcp_project_id --service_base_name xxxx --region xx-xxxxx --zone xxx-xxxxx-x --big_query_dataset xxxxx --domain domain.com --state /home/ubuntu/datalab-state/ --custom_certs_enabled false --ldap_host ldap_server_host --ldap_user cn=admin --ldap_bind_creds ldap_server_password --ldap_users_group ou=People
```

List of parameters for GKE cluster creation and Helm Charts deployment:

| Parameter                    | Description/Value                                                                     |
|------------------------------|---------------------------------------------------------------------------------------|
| conf\_service\_base\_name    | Any infrastructure value (should be unique)										   |
| gcp\_region                  | GCP region                                                                            |
| gcp\_zone                    | GCP zone                                                                              |
| gcp\_service\_account\_path  | Full path to auth json file                                                           |
| gcp\_project\_id             | ID of GCP project                                                                     |
| big\_query\_dataset 	       | Name of GCP billing dataset (BigQuery service)                                        |
| domain 	                   | Domain name                                                                           |,

After successful DataLab deployment You get output values which are needed for Keycloak configuration and Endpoint deployment, please save them.

Use direct link to Keycloak admin panel, as well as username and password for it. You have to login Keycloak admin panel and create user for DataLab. After that You are able to login into DataLab UI.

To proceed with DataLab resources creation Endpoint should be created.

To create Endpoint following steps should be executed:

- Create private and public key-pair for SSH access to endpoint instance
```
cd /home/ubuntu/incubator-datalab/infrastructure-provisioning/terraform/bin/
```
- Run python script to create DataLab Endpoint*:
```
python3 datalab.py deploy gcp endpoint --gcp_project_id gcp_project_id --creds_file /path/to/auth/file.json --key_name key_name --pkey /path/to/key/key_name.pem --service_base_name xxxx --path_to_pub_key /path/to/key/key_name.pub --endpoint_id yyyy --region xx-xxxxx --zone xxx-xxxxx-x --state /home/ubuntu/datalab-state/ --cloud_provider gcp --repository_user nexus_user --repository_pass nexus_password --repository_address nexus.develop.dlabanalytics.com --repository_port 8083 --vpc_id "vpc_id" --subnet_id "subnet_id" --ssn_ui_host domain.com --keycloak_auth_server_url https://service_base_name.domain.com/auth --keycloak_realm_name realm_name --keycloak_user_name admin_name --keycloak_user_password admin_password --keycloak_client_id client_id --keycloak_client_secret client_secret --step_root_ca "step_ca" --step_kid step_kid --step_kid_password --step_ca_url https://step_ca_url --mongo_password mongo_password --billing_dataset_name xxxxx --billing_enable True --ldap_host ldap_server_host --ldap_dn dc=example,dc=com --ldap_user cn=admin --ldap_bind_creds ldap_server_password --ldap_users_group ou=People
```
List of parameters for DataLab Endpoint creation:

| Parameter                    | Description/Value                                                                     |
|------------------------------|---------------------------------------------------------------------------------------|
| service\_base\_name          | Unique infrastructure value used for GKE cluster creation  						   |
| region                       | GCP region                                                                            |
| zone                         | GCP zone                                                                              |
| creds\_file                  | Full path to auth json file                                                           |
| gcp\_project\_id             | ID of GCP project                                                                     |
| pkey                         | Private SSH key						                            				   |
| path\_to\_pub                | Path to public SSH key                                                                |
| endpoint_id                  | Unique infrastructure value (6 characters max)                                        |
| repository\_*                | Please contact with DataLab team to get repository values                             |
| keycloak\_*                  | Use keycloak values form GKE cluster creation output                                  |
| step\_*                      | Use step\_ca values form GKE cluster creation output                                  |
| mongo\_password              | Use mongo\_password from GKE cluster creation output                                  |
| billing\_dataset\_name       | Name of GCP billing dataset (BigQuery service)                                        |
| domain 	                   | Domain name                                                                           |

*You may omit the following parameters (in case of using the same state path input variables are included from GKE createion output): --vpc_id "vpc_id" --subnet_id "subnet_id" --ssn_ui_host domain.com --keycloak_auth_server_url https://keycloak_auth_url --keycloak_realm_name realm_name --keycloak_user_name admin_name --keycloak_user_password admin_password --keycloak_client_id client_id --keycloak_client_secret client_secret --step_root_ca "step_ca" --step_kid step_kid --step_kid_password --step_ca_url https://step_ca_url 

After successful DataLab Endpoint deployment login into DataLab UI and add Endpoint at Environment Management page.
Use unique Endpoint name (it should be equal endpoint_id used for run endpoint deployment script) for DataLab UI and Endpoint URl in format: https://ip_address:8084/
