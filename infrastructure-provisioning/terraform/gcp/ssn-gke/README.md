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
- Install Terraform v0.12.3
- Install Git and clone DataLab repository

### Executing deployment scripts

Deployment of DataLab starts from GKE cluster creating.

To build GKE cluster with DataLab, following steps should be executed:

- Connect to the instance via SSH and run the following commands:

```
sudo su
apt-get update
wget https://releases.hashicorp.com/terraform/0.12.3/terraform_0.12.3_linux_amd64.zip
apt-get install unzip
unzip terraform_0.12.3_linux_amd64.zip
chmod +x terraform
mv terraform /usr/local/bin/
apt-get install jq
snap install kubectl --classic
mkdir /home/ubuntu/datalab-state
git clone https://github.com/apache/incubator-datalab.git
cd incubator-datalab/infrastructure-provisioning/terraform/gcp/ssn-gke/main/
git checkout develop
terraform init
```
- Run terraform apply command to create GKE cluster:

```
terraform apply -auto-approve -target=module.gke_cluster -state /home/ubuntu/datalab-state/terraform.tfstate -var credentials_file_path=/path/to/auth/file.json -var project_id=project_id -var service_base_name=datalab-xxxx -var region=xx-xxxxx -var zone=xxx-xxxxx-x -var big_query_dataset=test -var domain=k8s-gcp.domain.com
```
- Run terraform apply command to deploy Helm Charts:

```
terraform apply -auto-approve -target=module.helm_charts -state /home/ubuntu/datalab-state/terraform.tfstate -var credentials_file_path=/path/to/auth/file.json -var project_id=project_id -var service_base_name=xxxx -var region=xx-xxxxx -var zone=xxx-xxxxx-x -var big_query_dataset=test -var domain=k8s-gcp.domain.com
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

After successful Helm Charts deployment You will get direct link to Keycloak admin panel, as well as username and password for it. You have to login Keycloak admin panel and create user for DataLab. After that You will be able to login into DataLab UI.

To proceed with DataLab resources creation  Endpoint should be created and added in DataLab UI.

To create Endpoint following steps should be executed:

- Create private and public key-pair for SSH access to endpoint instance
```
cd /home/ubuntu/incubator-datalab/infrastructure-provisioning/terraform/bin/
```
- Run python script to deploy endpoint:
```
python3 datalab.py deploy gcp endpoint --gcp_project_id or2-msq-epmc-dlab-t1iylu --creds_file /path/to/auth/file.json --key_name key_name --pkey /path/to/key/key_name.pem --service_base_name xxxx --path_to_pub_key /path/to/key/key_name.pub --endpoint_id yyyy --region xx-xxxxx --zone xxx-xxxxx-x --ldap_host lsap_server_host --ldap_dn dc=example,dc=com --ldap_user cn=admin --ldap_bind_creds ldap_server_password --ldap_users_group ou=People --state /home/ubuntu/datalab-state/ --cloud_provider gcp --repository_user nexus_user --repository_pass nexus_password --repository_address nexus.develop.dlabanalytics.com --repository_port 8083 --vpc_id "vpc_id" --subnet_id "subnet_id" --ssn_ui_host sbn.k8s-gcp.domain.com --keycloak_auth_server_url https://sbn.k8s-gcp.k8s-gcp.domain.com/auth --keycloak_realm_name realm_name --keycloak_user_name admin_name --keycloak_user_password admin_password --keycloak_client_id client_id --keycloak_client_secret client_secret --step_root_ca "step_ca" --step_kid step_kid --step_kid_password mFYNNmtV --step_ca_url https://step_ca_url --mongo_password none --billing_dataset_name test --billing_enable True
```
