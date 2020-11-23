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
mkdir /home/ubuntu/datalab-state
git clone https://github.com/apache/incubator-datalab.git
cd incubator-datalab/infrastructure-provisioning/terraform/gcp/ssn-gke/main/
git checkout DATALAB-2102
terraform init
```
- Run terraform apply command to create GKE cluster:

```
terraform apply -auto-approve -target=module.gke_cluster -state /home/ubuntu/datalab-state/terraform.tfstate -var credentials_file_path=/path/to/auth/file.json -var project_id=project_id -var service_base_name=datalab-xxxx -var region=xx-xxxxx -var zone=xxx-xxxxx-x -var big_query_dataset=test -var domain=k8s-gcp.domain.com
```

- Run terraform apply command to deploy Helm Charts:

```
terraform apply -auto-approve -target=module.helm_charts -state /home/ubuntu/datalab-state/terraform.tfstate -var credentials_file_path=/path/to/auth/file.json -var project_id=project_id -var service_base_name=datalab-xxxx -var region=xx-xxxxx -var zone=xxx-xxxxx-x -var big_query_dataset=test -var domain=k8s-gcp.domain.com
```

List of parameters for GKE cluster creation and Helm Charts deployment:

| Parameter                    | Description/Value                                                                     |
|------------------------------|---------------------------------------------------------------------------------------|
| conf\_service\_base\_name    | Any infrastructure value (should be unique)										   |
| gcp\_region                  | GCP region                                                                            |
| gcp\_zone                    | GCP zone                                                                              |
| gcp\_service\_account\_path  | Full path to auth json file                                                           |
| gcp\_project\_id             | ID of GCP project                                                                     |
| billing\_dataset\_name 	   | Name of GCP dataset (BigQuery service)                                                |,

After successful Helm Charts deployment You will get direct link to Keycloak admin panel, as well as username and password for it. You have to login Keycloak admin panel and create user for DataLab. After that You will be able to login into DataLab UI.

To proceed with DataLab resources creation  Endpoint should be created and added in DataLab UI.

To create Endpoint following steps should be executed: