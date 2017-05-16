DLAB Overview
=============

-------
CONTENTS
-------

[What is DLAB?](#What_is_DLAB)

[Logical architecture](#Logical_architecture)

[Physical architecture](#Physical_architecture)

[DLab Deployment](#DLab_Deployment)

&nbsp; &nbsp; &nbsp; &nbsp; [Structure of main DLab directory](#DLab_directory)

&nbsp; &nbsp; &nbsp; &nbsp; [Structure of log directory](#log_directory)

&nbsp; &nbsp; &nbsp; &nbsp; [Self-Service Node](#Self_Service_Node)

&nbsp; &nbsp; &nbsp; &nbsp; [Edge Node](#Edge_Node)

&nbsp; &nbsp; &nbsp; &nbsp; [Notebook node](#Notebook_node)

&nbsp; &nbsp; &nbsp; &nbsp; [EMR cluster](#EMR_cluster)

&nbsp; &nbsp; &nbsp; &nbsp; [Configuration files](#Configuration_files)

&nbsp; &nbsp; &nbsp; &nbsp; [Starting/Stopping services](#Starting_Stopping_services)

&nbsp; &nbsp; &nbsp; &nbsp; [Troubleshooting](#Troubleshooting)

[Development](#Development)

&nbsp; &nbsp; &nbsp; &nbsp; [Folder structure](#Folder_structure)

&nbsp; &nbsp; &nbsp; &nbsp; [Pre-requisites](#Pre-requisites)

&nbsp; &nbsp; &nbsp; &nbsp; [Java back-end services](#Java_back_end_services)

&nbsp; &nbsp; &nbsp; &nbsp; [Front-end](#Front_end)

&nbsp; &nbsp; &nbsp; &nbsp; [How to setup local development environment](#setup_local_environment)

&nbsp; &nbsp; &nbsp; &nbsp; [How to run locally](#run_locally)

&nbsp; &nbsp; &nbsp; &nbsp; [Infrastructure provisioning](#Infrastructure_provisioning)
 
---------------
# What is DLAB? <a name="What_is_DLAB"></a>

DLab is an essential toolset for analytics. It is a self-service Web Console, used to create and manage exploratory environments. It allows teams to spin up analytical environments with best of breed open-source tools just with a single click of the mouse. Once established, environment can be managed by an analytical team itself, leveraging simple and easy-to-use Web Interface.

----------------------------
# Logical architecture <a name="Logical_architecture"></a>

The following diagram demonstrates high-level logical architecture of DLab.

![Logical architecture](doc/logical_architecture.png)

The diagram shows main components of DLab, which is a self-service for the infrastructure deployment and interaction with it. The purpose of each component is described below.

## Self-Service

Self-Service is a service, which provides RESTful user API with Web User Interface for data scientist. It tightly interacts with Provisioning Service and Database. Self-Service delegates all user\`s requests to Provisioning Service. After execution of certain request from Self-service, Provisioning Service returns response about corresponding action happened with particular resource. Self-service, then, saves this response into Database. So, each time Self-Service receives request about status of provisioned infrastructure resources – it loads it from Database and propagates to Web UI.

## Provisioning Service

The Provisioning Service is a RESTful service, which provides APIs for provisioning of the user’s infrastructure. Provisioning Service receives the request from Self-Service, afterwards it forms and sends a command to the docker to execute requested action. Docker executes the command and generates a response.json file. Provisioning service analyzes response.json and responds to initial request of Self-Service, providing status-related information of the instance.

## Security service

Security Service is RESTful service, which provides authorization API for Self-Service and Provisioning Service via LDAP.

## Docker

Docker is an infrastructure-provisioning module based on Docker service, which provides low-level actions for infrastructure management.

## Database

Database serves as a storage with description of user infrastructure, user’s settings and service information.

-----------------------------
# Physical architecture <a name="Physical_architecture"></a>

The following diagram demonstrates high-level physical architecture of DLab

![Physical architecture](doc/physical_architecture.png)

## Main components

-   Self-service node (SSN)
-   Edge node
-   Notebook node (Jupyter, Rstudio, etc.)
-   EMR cluster

## Self-service node (SSN)

Creation of self-service node – is the first step for deploying DLab. SSN is a main server with following pre-installed services:

-   DLab Web UI – is Web user interface for managing/deploying all components of DLab. It is accessible by the following URL: http://SSN\_Public\_IP\_or\_Public\_DNS
-   MongoDB – is a database, which contains part of DLab’s configuration, user’s exploratory environments description as well as user’s preferences.
-   Docker – used for building DLab Docker containers, which will be used for provisioning other components.
-   Jenkins – is an alternative to Web UI. It is accessible by the following link: http://SSN\_Public\_IP\_or\_Public\_DNS/jenkins

## Edge node

Setting up Edge node is the first step that user is asked to do once logged into DLab. This node is used as proxy server and SSH gateway for the user. Through Edge node users can access Notebook via HTTP and SSH. Edge Node has a Squid HTTP web proxy pre-installed.

## Notebook node

The next step is setting up a Notebook node (or a Notebook server). It is a server with pre-installed applications and libraries for data processing, data cleaning and transformations, numerical simulations, statistical modeling, machine learning, etc. Following analytical tools are currently supported in DLab and can be installed on a Notebook node:

-   Jupyter
-   R-studio
-   Zeppelin
-   Jupyter + TensorFlow

Apache Spark is also installed for each of the analytical tools above.

## EMR cluster

After deploying Notebook node, user can create EMR cluster for it. EMR cluster is a managed cluster platform, that simplifies running big data frameworks, such as Apache Hadoop and Apache Spark on AWS to process and analyze vast amounts of data. Adding EMR is not mandatory and is only needed in case additional computational resources are required for job execution.

----------------------
# DLab Deployment <a name="DLab_Deployment"></a>

## Structure of main DLab directory <a name="DLab_directory"></a>

DLab’s SSN node main directory structure is as follows:

    /opt  
     └───dlab  
         ├───conf  
         ├───sources  
         ├───template  
         ├───tmp  
         │   └───result  
         └───webapp  

-   conf – contains configuration for DLab Web UI and back-end services;
-   sources – contains all Docker/Python scripts, templates and files for provisioning;
-   template – docker’s templates;
-   tmp –temporary directory of DLab;
-   tmp/result – temporary directory for Docker’s response files;
-   webapp – contains all .jar files for DLab Web UI and back-end
    services.

## Structure of log directory <a name="log_directory"></a>

SSN node structure of log directory is as follows:

    /var
     └───opt
         └───dlab
             └───log
                 ├───edge
                 ├───emr
                 ├───notebook
                 └───ssn

These directories contain the log files for each template and for DLab back-end services.
-   ssn – contains logs of back-end services;
-   provisioning.log – Provisioning Service log file;
-   security.log – Security Service log file;
-   selfservice.log – Self-Service log file;
-   edge, notebook, emr – contains logs of Python scripts.

## Self-Service Node <a name="Self_Service_Node"></a>

### Create

To build SSN node, following steps should be executed:

1.  Clone Git repository and make sure that all [pre-requisites](#Pre-requisites) are installed.
2.  Go to *dlab* directory.
3.  Execute following script:
```
/usr/bin/python infrastructure-provisioning/scripts/deploy_dlab.py --infrastructure_tag dlab_test --access_key_id XXXXXXX --secret_access_key XXXXXXXXXX --region us-west-2 --os_family debian --cloud_provider aws --os_user ubuntu --vpc_id vpc-xxxxx --subnet_id subnet-xxxxx --sg_ids sg-xxxxx,sg-xxxx --key_path /root/ --key_name Test --tag_resource_id dlab --action create
```

This bash script will build front-end and back-end part of DLab, create SSN docker image and run Docker container for creating SSN node.

List of parameters for SSN node deployment:

| Parameter           | Description/Value                                                                       |
|---------------------|-----------------------------------------------------------------------------------------|
| infrastructure\_tag | Any infrastructure value (should be unique if multiple SSN’s have been deployed before) |
| access\_key\_id     | AWS user access key                                                                     |
| secret\_access\_key | AWS user secret access key                                                              |
| region              | AWS region                                                                              |
| os\_family          | Name of the Linux distributive family, which is supported by DLab (Debian/RedHat)       |
| cloud\_provider     | Name of the cloud provider, which is supported by DLab (AWS)                            |
| os\_user            | Name of OS user. By default for Debian – **ubuntu**, for RedHat – **ec2-user**          |
| vpc\_id             | ID of the Virtual Private Cloud (VPC)                                                   |
| subnet\_id          | ID of the public subnet                                                                 |
| sg\_ids             | One or more ID\`s of AWS Security Groups, which will be assigned to SSN node            |
| key\_path           | Path to admin key (without key name)                                                    |
| key\_name           | Name of the uploaded SSH key file (without “.pem” extension)                            |
| tag\_resource\_id   | The name of tag for billing reports                                                     |
| action              | In case of SSN node creation, this parameter should be set to “create”                  |

**Note:** If the following parameters are not specified, they will be created automatically:
-   vpc\_id
-   subnet\_id
-   sg\_ids

After SSN node deployment following AWS resources will be created:

-   EC2 instance
-   Security Group for SSN node (if it was specified, script will attach the provided one)
-   IAM role and Instance Profile
-   VPC, Subnet (if they have not been specified)
-   S3 bucket – its name will be \<service\_base\_name\>-ssn. This bucket will contain necessary dependencies and configuration files for Notebook nodes (such as .jar files, YARN configuration, etc.)

### Terminate

Terminating SSN node will also remove all nodes and components related to it. Basically, terminating Self-service node will terminate all DLab’s infrastructure.
Example of command for terminating DLab environment:
```
/usr/bin/python infrastructure-provisioning/scripts/deploy_dlab.py --infrastructure_tag dlab_test --access_key_id XXXXXXX --secret_access_key XXXXXXXXXX --region us-west-2 --key_path /root/ --key_name Test --action terminate
```
List of parameters for SSN node termination:

| Parameter           | Description/Value                                            |
|---------------------|--------------------------------------------------------------|
| infrastructure\_tag | Unique infrastructure value                                  |
| access\_key\_id     | AWS user access key                                          |
| secret\_access\_key | AWS user secret access key                                   |
| region              | AWS region                                                   |
| key\_path           | Path to admin key (without key name)                         |
| key\_name           | Name of the uploaded SSH key file (without “.pem” extension) |
| action              | create                                                       |

## Edge Node <a name="Edge_Node"></a>

Gateway node (or an Edge node) is an AWS EC2 Instance provisioned in a public subnet. It serves as an entry point for accessing user’s personal analytical environment. It is created by an end-user, whose public key will be uploaded there. Only via Edge node, DLab user can access such application resources as notebook servers and EMR clusters. Also, Edge Node is used to setup SOCKS proxy to access notebook servers via Web UI and SSH. Elastic IP address is assigned to an Edge Node. In case Edge node instance has been removed by mistake, there is an option to re-create it and Edge node IP address won’t chang.

### Create

In order to create Edge node using DLab Web UI – login and, click on the button “Upload”. Choose user’s SSH public key and after that click on the button “Create”. Edge node will be deployed and corresponding EC2 instance will be started.

The following AWS resources will be created:
-   Edge EC2 instance
-   S3 user bucket
-   Security Group for user's Edge instance
-   Security Group for all further user's Notebook instances
-   IAM Roles and Instance Profiles for user's Edge instance
-   IAM Roles and Instance Profiles all further user's Notebook instances
-   Private subnet. All further nodes (Notebooks, EMR clusters) will be in different subnet than SSN.

List of parameters for Edge node creation:

| Parameter                  | Description/Value                                                                 |
|----------------------------|-----------------------------------------------------------------------------------|
| conf\_resource             | edge                                                                              |
| conf\_os\_user             | Name of the SSH user                                                              |
| conf\_os\_family           | Name of the Linux distributive family, which is supported by DLAB (debian/redhat) |
| conf\_service\_base\_name  | Unique infrastructure value, specified during SSN deployment                      |
| conf\_key\_name            | Name of the uploaded SSH key file (without ".pem")                                |
| edge\_user\_name           | Name of the user                                                                  |
| aws\_vpc\_id               | ID of AWS VPC where infrastructure is being deployed                              |
| aws\_region                | AWS region where infrastructure was deployed                                      |
| aws\_security\_groups\_ids | One or more id’s of the SSN instance security group                               |
| aws\_subnet\_id            | ID of the AWS public subnet where Edge will be deployed                           |
| aws\_iam\_user             | Name of AWS IAM user                                                              |
| conf\_tag\_resource\_id          | The name of tag for billing reports                                                      |
| action                     | create                                                                            |

### Start/Stop <a name=""></a>

To start/stop Edge node, click on the button which looks like a cycle on the top right corner, then click on the button which is located in “Action” field and in the drop-down menu click on the appropriate action.

List of parameters for Edge node starting/stopping:

| Parameter                 | Description/Value                                            |
|---------------------------|--------------------------------------------------------------|
| conf\_resource            | edge                                                         |
| conf\_service\_base\_name | Unique infrastructure value, specified during SSN deployment |
| edge\_user\_name          | Name of the user                                             |
| aws\_region               | AWS region where infrastructure was deployed                 |
| action                    | start/stop                                                   |

### Recreate <a name=""></a>

In case Edge node was damaged, or terminated manually, there is an option to re-create it.

If Edge node was removed for some reason, to re-create it, click on the status button close to logged in users’s name (top right corner of the screen).Then click on gear icon in Actions column and choose “Recreate”.

List of parameters for Edge node recreation:

| Parameter                  | Description/Value                                                                 |
|----------------------------|-----------------------------------------------------------------------------------|
| conf\_resource             | edge                                                                              |
| conf\_os\_user             | Name of the SSH user                                                              |
| conf\_os\_family           | Name of the Linux distributive family, which is supported by DLAB (Debian/RedHat) |
| conf\_service\_base\_name  | Unique infrastructure value, specified during SSN deployment                      |
| conf\_key\_name            | Name of the uploaded SSH key file (without ".pem")                                |
| edge\_user\_name           | Name of the user                                                                  |
| aws\_vpc\_id               | ID of AWS VPC where infrastructure is being deployed                              |
| aws\_region                | AWS region where infrastructure was deployed                                      |
| aws\_security\_groups\_ids | ID of the SSN instance's AWS security group                                       |
| aws\_subnet\_id            | ID of the AWS public subnet where Edge was deployed                               |
| aws\_iam\_user             | Name of AWS IAM user                                                              |
| edge\_elastic\_ip          | AWS Elastic IP address which was associated to Edge node                          |
| conf\_tag\_resource\_id          | The name of tag for billing reports                                                      |
| action                     | Create                                                                            |

## Notebook node <a name="Notebook_node"></a>

Notebook node is an AWS EC2 instance, with preinstalled analytical software, needed dependencies and with pre-configured kernels and interpreters. It is the main part of personal analytical environment, which is setup by a data scientist. It can be Created, Stopped and Terminated. To support variety of analytical needs - Notebook node can be provisioned on any of AWS supported EC2 instance shape for your particular region. From analytical software, which is already pre-installed on a notebook node, end users can access (read/write) data stored on S3 buckets.

### Create

To create Notebook node, click on the “Create new” button. Then, in drop-down menu choose template type (jupyter/rstudio/zeppelin/tensor), enter notebook name and choose instance shape. After clicking the button “Create”, notebook node will be deployed and started.

List of parameters for Notebook node creation:

| Parameter                     | Description/Value                                                                 |
|-------------------------------|-----------------------------------------------------------------------------------|
| conf\_resource                | notebook                                                                          |
| conf\_os\_user                | Name of the SSH user                                                              |
| conf\_os\_family              | Name of the Linux distributive family, which is supported by DLAB (debian/redhat) |
| conf\_service\_base\_name     | Unique infrastructure value, specified during SSN deployment                      |
| conf\_key\_name               | Name of the uploaded SSH key file (without ".pem")                                |
| edge\_user\_name              | Value that previously was used when Edge being provisioned                        |
| aws\_notebook\_instance\_type | Value of the Notebook EC2 instance shape                                          |
| aws\_region                   | AWS region where infrastructure was deployed                                      |
| aws\_security\_groups\_ids    | ID of the SSN instance's security group                                           |
| application                   | Type of the notebook template (jupyter/rstudio/zeppelin/tensor)                   |
| conf\_tag\_resource\_id             | The name of tag for billing reports                                                         |
| action                        | Create                                                                            |

### Stop

In order to stop Notebook node, click on the “gear” button in Actions column. From the drop-down menu click on “Stop” action.

List of parameters for Notebook node stopping:

| Parameter                 | Description/Value                                            |
|---------------------------|--------------------------------------------------------------|
| conf\_resource            | notebook                                                     |
| conf\_os\_user            | Name of the SSH user                                         |
| conf\_service\_base\_name | Unique infrastructure value, specified during SSN deployment |
| conf\_key\_name           | Name of the uploaded SSH key file (without ".pem")           |
| edge\_user\_name          | Value that previously was used when Edge being provisioned   |
| notebook\_instance\_name  | Name of the Notebook instance to terminate                   |
| aws\_region               | AWS region where infrastructure was deployed                 |
| action                    | Stop                                                         |

### Start/Terminate

In order to start/terminate Notebook node, click on the button, which looks like gear in “Action” field. Then in drop-down menu choose “Start” or “Terminate” action.

List of parameters for Notebook node start/termination:

| Parameter                 | Description/Value                                            |
|---------------------------|--------------------------------------------------------------|
| conf\_resource            | notebook                                                     |
| conf\_service\_base\_name | Unique infrastructure value, specified during SSN deployment |
| conf\_key\_name           | Name of the uploaded SSH key file (without ".pem")           |
| edge\_user\_name          | Value that previously was used when Edge being provisioned   |
| notebook\_instance\_name  | Name of the Notebook instance to terminate                   |
| aws\_region               | AWS region where infrastructure was deployed                 |
| action                    | Stop                                                         |

**Note:** If terminate action is called, all connected EMR clusters will be removed.

### List/Install additional libraries

In order to list available libraries (OS/Python2/Python3/R) on Notebook node, click on the button, which looks like gear in “Action” field. Then in drop-down menu choose “Install additional libs” action.

List of parameters for Notebook node to **get list** of available libraries:

| Parameter                     | Description/Value                                                                 |
|-------------------------------|-----------------------------------------------------------------------------------|
| conf\_resource                | notebook                                                                          |
| request\_id                   | Unique request id                                                              |
| conf\_os\_user                | Name of the SSH user                                                              |
| conf\_os\_family              | Name of the Linux distributive family, which is supported by DLAB (debian/redhat) |
| conf\_service\_base\_name     | Unique infrastructure value, specified during SSN deployment                      |
| conf\_key\_name               | Name of the uploaded SSH key file (without ".pem")                                |
| edge\_user\_name              | Value that previously was used when Edge being provisioned                        |
| notebook\_instance\_name      | Name of the Notebook instance to terminate                   |
| aws\_region                   | AWS region where infrastructure was deployed                                      |
| application                   | Type of the notebook template (jupyter/rstudio/zeppelin/tensor/deeplearning)      |
| action                        | list_libs                                                                           |

**Note:** This operation will returns a file with response **[edge_user_name]\_[application]\_[request_id]\_all\_pkgs.json**


List of parameters for Notebook node to **install** additional libraries:

| Parameter                     | Description/Value                                                                 |
|-------------------------------|-----------------------------------------------------------------------------------|
| conf\_resource                | notebook                                                                          |
| request\_id                   | Unique request id                                                              |
| conf\_os\_user                | Name of the SSH user                                                              |
| conf\_os\_family              | Name of the Linux distributive family, which is supported by DLAB (debian/redhat) |
| conf\_service\_base\_name     | Unique infrastructure value, specified during SSN deployment                      |
| conf\_key\_name               | Name of the uploaded SSH key file (without ".pem")                                |
| edge\_user\_name              | Value that previously was used when Edge being provisioned                        |
| notebook\_instance\_name      | Name of the Notebook instance to terminate                   |
| aws\_region                   | AWS region where infrastructure was deployed                                      |
| application                   | Type of the notebook template (jupyter/rstudio/zeppelin/tensor/deeplearning)      |
| additional_libs               | List of additional libraries in JSON format with type (os_pkg/pip2/pip3/r_pkg)    |
| action                        | install_libs                                                                       |

**Example** of additional_libs parameter:

```
{'libraries': {
    'os_pkg': ['nmap', 'htop'],
    'pip2': ['requests', 'configparser'],
    'pip3': ['configparser'],
    'r_pkg': ['rmarkdown']
    }
}
```

**Note:** This JSON string must be with single quotes.

**Note:** Response file contains received lists of all and failed libraries.

## EMR cluster <a name="EMR_cluster"></a>

EMR cluster can be created if more computational resources are needed for executing analytical algorithms and models, triggered from analytical tools. Jobs execution will be scaled to a cluster mode increasing the performance and decreasing execution time.

### Create

To create EMR cluster click on the “gear” button in Actions column, and click on “Deploy EMR”. Specify EMR version, fill in EMR name, specify number of instances and instance shapes. Click on the “Create” button.

List of parameters for EMR cluster creation:

| Parameter                   | Description/Value                                            |
|-----------------------------|--------------------------------------------------------------|
| conf\_resource              | emr                                                          |
| conf\_os\_user              | Name of the SSH user                                         |
| conf\_service\_base\_name   | Unique infrastructure value, specified during SSN deployment |
| conf\_key\_name             | Name of the uploaded SSH key file (without ".pem")           |
| emr\_timeout                | Value of timeout for EMR during build.                       |
| emr\_instance\_count        | Amount of instance in cluster                                |
| emr\_master\_instance\_type | Value for EMR EC2 master instance shape                      |
| emr\_slave\_instance\_type  | Value for EMR EC2 slave instances shapes                     |
| emr\_version                | Available versions of EMR (emr-5.2.0/emr-5.3.1)              |
| notebook\_instance\_name    | Name of the Notebook EMR will be linked to                   |
| edge\_user\_name            | Value that previously was used when Edge being provisioned   |
| aws\_region                 | AWS region where infrastructure was deployed                 |
| conf\_tag\_resource\_id           | The name of tag for billing reports                          |
| action                      | create                                                       |

**Note:** If “Spot instances” is enabled, EMR Slave nodes will be created as EC2 Spot instances.

### Terminate

In order to terminate EMR cluster, click on “x” button which is located in “Computational resources” field.

List of parameters for EMR cluster termination:

| Parameter                 | Description/Value                                            |
|---------------------------|--------------------------------------------------------------|
| conf\_resource            | emr                                                          |
| conf\_os\_user            | Name of the SSH user                                         |
| conf\_service\_base\_name | Unique infrastructure value, specified during SSN deployment |
| conf\_key\_name           | Name of the uploaded SSH key file (without ".pem")           |
| edge\_user\_name          | Value that previously was used when Edge being provisioned   |
| emr\_cluster\_name        | Name of the EMR to terminate                                 |
| notebook\_instance\_name  | Name of the Notebook instance which EMR is linked to         |
| aws\_region               | AWS region where infrastructure was deployed                 |
| action                    | Terminate                                                    |

## Configuration files <a name="Configuration_files"></a>

DLab configuration files are located on SSN node by following path:
-   /opt/dlab/conf ssn.yml – basic configuration for all java services;
-   provisioning.yml – Provisioning Service configuration file;for
-   security.yml – Security Service configuration file;
-   self-service.yml – Self-Service configuration file.

## Starting/Stopping services <a name="Starting_Stopping_services"></a>

All DLab services running as OS services and have next syntax for
starting and stopping:
```
sudo supervisorctl {start | stop | status} [all | provserv | secserv | ui]
```
-   start – starting service or services;
-   stop – stopping service or services;
-   status – show status of service or services;
-   all – execute command for all services, this option is default;
-   provserv – execute command for Provisioning Service;
-   secserv – execute command for Security Service;
-   ui – execute command for Self-Service.

## Troubleshooting <a name="Troubleshooting"></a>

If the parameter dlab\_path of configuration file dlab.ini wasn’t changed, the path to DLab service would default to:

-   /opt/dlab/ - main directory of DLab service
-   /var/opt/dlab/log/ or /var/log/dlab/ - path to log files

To check logs of Docker containers run the following commands:
```
docker ps -a – to get list of containers which were executed.
...
a85d0d3c27aa docker.dlab-emr:latest "/root/entrypoint...." 2 hours ago Exited (0) 2 hours ago infallible_gallileo
6bc2afeb888e docker.dlab-jupyter:latest "/root/entrypoint...." 2 hours ago Exited (0) 2 hours ago practical_cori
51b71c5d4aa3 docker.dlab-zeppelin:latest "/root/entrypoint...." 2 hours ago Exited (0) 2 hours ago determined_knuth
...
docker logs <container_id> – to get log for particular Docker container.
```
To change Docker images on existing environment, execute following steps:

1.  SSH to SSN instance
2.  go to */opt/dlab/sources/*
3.  Modify needed files
4.  Rebuild proper Docker images, using one or several commands (depending on what files you’ve changed):
```
docker build --build-arg OS=<os_family> --build-arg CLOUD=<cloud_provider> --file base/Dockerfile -t docker.dlab-base .
docker build --build-arg OS=<os_family> --build-arg CLOUD=<cloud_provider> --file edge/Dockerfile -t docker.dlab-edge .
docker build --build-arg OS=<os_family> --build-arg CLOUD=<cloud_provider> --file jupyter/Dockerfile -t docker.dlab-jupyter .
docker build --build-arg OS=<os_family> --build-arg CLOUD=<cloud_provider> --file rstudio/Dockerfile -t docker.dlab-rstudio .
docker build --build-arg OS=<os_family> --build-arg CLOUD=<cloud_provider> --file zeppelin/Dockerfile -t docker.dlab-zeppelin .
docker build --build-arg OS=<os_family> --build-arg CLOUD=<cloud_provider> --file emr/Dockerfile -t docker.dlab-emr .
```

----------------
# Development <a name="Development"></a>

DLab services could be ran in development mode. This mode emulates real work an does not create any EC2 instances in Amazon Cloud.

## Folder structure <a name="Folder_structure"></a>

    dlab
    ├───infrastructure-provisioning
    └───services
        ├───common
        ├───provisioning-service
        ├───security-service
        └───self-service

-   infrastructure-provisioning – code of infrastructure-provisioning module;
-   services – code of back-end services;
-   common – reusable code for all services;
-   provisioning-service – Provisioning Service;
-   security-service – Security Service;
-   self-service – Self-Service and UI;

## Pre-requisites <a name="Pre-requisites"></a>

In order to start development of Front-end Web UI part of DLab - Git repository should be cloned and the following packages should be installed:

-   Git 1.7 or higher
-   Maven 3.3 or higher
-   Python 2.7
-   Mongo DB 3.0 or higher
-   Docker 1.12 - Infrastructure provisioning
-   Java Development Kit 8 – Back-end
-   Node.js 6.1 or higher - WebUI
-   Angular CLI v1.0.0-rc.1 or higher - WebUI
-   TypeScript v2.0 or higher - WebUI
-   Angular2 v2.4 – WebUI
-   Development IDE (Eclipse or Intellij IDEA)

## Java back-end services <a name="Java_back_end_services"></a>

### Java components description

#### Common

Common is a module, which wraps set of reusable code over services. Commonly reused functionality is as follows:

1.  Models
2.  REST client
3.  Mongo persistence DAO
4.  Security models and DAO

#### Self-Service

Self-Service provides REST based API’s. It tightly interacts with Provisioning Service and Security Service and actually delegates most of user\`s requests for execution.

| API name                        | Supported actions                                   | Description            |
|---------------------------------|-----------------------------------------------------|------------------------|
| SecurityResource                | Login<br>Authorize<br>Logout                        | User’s authentication. |
| EdgeResource                    | Start<br>Stop<br>Status                             | Manage EDGE node.      |
| KeyUploaderResource             | Check key<br>Upload key<br>Recover<br>Callback      | Used for Gateway/EDGE node public key upload and further storing of this information in Mongo DB. |
| UserSettingsResource            | Get settings<br>Save settings                       | User’s preferences.    |
| InfrastructureResource          | Get status of infrastructure<br>Updates the status of the resources | Used for obtaining Edge Node status, as well as for updating the statuses of provisioned resources. |
| InfrastructureProvisionResource | Get user provisioning resources<br>Get computation resources templates<br>Get exploratory environment templates | Used for propagating the list of provisioned resources per user. |
| ExploratoryResource             | Create<br>Status<br>Start<br>Stop<br>Terminate      | Used for exploratory environment management. |
| ComputationalResource           | Limits<br>Create<br>Status<br>Terminate             | Used for computational resources management. |

#### Provisioning Service

The Provisioning Service is key, REST based service for management of AWS/Docker based environment resources like computational, exploratory,
edge, etc.

| API name               | Supported actions                    | Description                                                                  |
|------------------------|--------------------------------------|------------------------------------------------------------------------------|
| DockerResource         | Get Docker image<br>Run Docker image | Requests and describes Docker images and templates.                          |
| EdgeResource           | Create<br>Start<br>Stop              | Provides Docker actions for EDGE node management.                            |
| ExploratoryResource    | Create<br>Start<br>Stop<br>Terminate | Provides Docker actions for working with exploratory environment management. |
| ComputationalResource  | Create<br>Terminate                  | Docker actions for computational resources management.                       |
| InfrastructureResource | Status                               | Docker action for obtaining status of DLab infrastructure instances.         |

#### Security service

Security service is REST based service for user authentication over LDAP and in AWS.

| API name           | Supported actions                                     | Description                              |
|--------------------|-------------------------------------------------------|------------------------------------------|
| LdapAuthentication | Gateway node (or an Edge node) is an AWS EC2 Instance | Used for authentication in LDAP and AWS. |

## Front-end <a name="Front_end"></a>

### Front-end components description

Web UI sources are part of Self-Service.

Sources are located in dlab/services/self-service/src/main/resources/webapp

| Main pages                    | Components and Services |
|-------------------------------|-------------------------|
| Login page                    | LoginComponent<br>*applicationSecurityService* handles http calls and stores authentication tokens on the client and attaches the token to authenticated calls;<br>*healthStatusService* and *appRoutingService* check instances states and redirect to appropriate page. |
| Home page (list of resources) | HomeComponent<br>nested several main components like ResourcesGrid for notebooks data rendering and filtering, using custom MultiSelectDropdown component;<br>multiple modal dialogs components used for new instances creation, displaying detailed info and actions confirmation. |
| Health Status page            | HealthStatusComponent<br>*HealthStatusGridComponent* displays list of instances, their types, statutes, ID’s and uses *healthStatusService* for handling main actions. |
| Help pages                    | Static pages that contains information and instructions on how to access Notebook Server and generate SSH key pair. Includes only *NavbarComponent*. |
| Error page                    | Simple static page letting users know that opened page does not exist. Includes only *NavbarComponent*. | 

## How to setup local development environment <a name="setup_local_environment"></a>

The development environment setup description is written with assumption that user already has installed Java8 (JDK), Maven3 and set environment variables (JAVA\_HOME, M2\_HOME).­­­­­­ The description will cover Mongo installation, Mongo user creation, filling initial data into Mongo, Node.js installation

### Install Mongo database

  * Download MongoDB from [https://www.mongodb.com/download-center](https://www.mongodb.com/download-center)
  * Install database based on MongoDB instructions
  * Start DB server and create accounts
```
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
use <database_name>
db.createUser(
   {
     user: "admin",
     pwd: "<password>",
     roles: [ "dbAdmin", "userAdmin", "readWrite" ]
   }
)
```

  * Load collections form file dlab/services/mongo\_settings.json

```
mongoimport -u admin -p <password> -d <database_name> –c settings mongo_settings.json
```

### Setting up environment options

  * Set option DEV\_MODE to **true**, mongo database name and password in configuration file dlab/infrastructure-provisioning/src/ssn/templates/ssn.yml

```
#DEV_MODE="true"
...
mongo:
  database: <database_name>
  password: <password>
```

  * Add system environment variable DLAB\_CONF\_DIR=&lt;dlab\_root\_folder&gt;/dlab/infrastructure-provisioning/src/ssn/templates/ssn.yml or create two symlinks in dlab/services/provisioning-service and dlab/services/self-service folders for file dlab/infrastructure-provisioning/src/ssn/templates/ssn.yml.

*Unix*

```
ln -s ssn.yml ../../infrastructure-provisioning/src/ssn/templates/ssn.yml\
```

*Windows*

```
mklink ssn.yml ..\\..\\infrastructure-provisioning\\src\\ssn\\templates\\ssn.yml
```

  * For Unix system create two folders and grant permission for writing:

```
/var/opt/dlab/log/ssn
/opt/dlab/tmp/result
```

### Install Node.js

  * Download Node.js from [https://nodejs.org/en](https://nodejs.org/en)
  * Install Node.js
  * Make sure that the installation folder of Node.js has been added  to the system environment variable **PATH**
  * Install latest packages

```
npm install npm@latest –g
```

### Build Web UI components

  * Change folder to dlab/services/self-service/src/main/resources/webapp and install the dependencies from a package.json manifest

```
npm install
```

  * Build web application

```
npm run build.prod
```

## How to run locally <a name="run_locally"></a>

There is a possibility to run Self-Service and Provisioning Service locally. All requests from Provisioning Service to Docker are mocked and instance creation status will be persisted to Mongo (only without real impact on Docker and AWS). Security Service can\`t be running on local machine because of local LDAP mocking complexity.

Both services, Self-Service and Provisioning Service are dependent on dlab/provisioning-infrastructure/ssn/templates/ssn.yml configuration file. Both services have main functions as entry point, SelfServiceApplication for Self-Service and ProvisioningServiceApplication for Provisioning Service. Services could be started by running main methods of these classes. Both main functions require two arguments:

  * Run mode (“server”)
  * Configuration file name (“self-service.yml” or “provisioning.yml”  depending on the service). Both files are located in root service directory. These configuration files contain service settings and are ready to use.

The services start up order does matter. Since Self-Service depends on Provisioning Service, the last should be started first and Self-Service afterwards. Services could be started from local IDEA (Eclipse or Intellij Idea) “Run” functionality of toolbox.

Run application flow is following:

  * Run provisioning-service passing 2 arguments: server, self-service.yml
  * Run self-service passing 2 arguments: server, provisioning.yml
  * Try to access self-service Web UI by http://localhost:8080

```
User: test
Password: <any>
```

## Infrastructure provisioning <a name="Infrastructure_provisioning"></a>

### DevOps components overview

The following list shows common structure of scripts for deploying DLab

#### Folder structure

    dlab
    └───infrastructure-provisioning
        └───src
            ├───base
            ├───edge
            ├───emr
            ├───general
            ├───jupyter
            ├───rstudio
            ├───ssn
            ├───tensor
            └───zeppelin

Each directory except *general* contains Python scripts, Docker files, templates, files for appropriate Docker image.

-   base – Main Docker image. It is a common/base image for other ones.
-   edge – Docker image for Edge node.
-   emr – Docker image for EMR cluster.
-   general – OS and CLOUD dependent common source.
-   ssn – Docker image for Self-Service node (SSN).
-   jupyter/rstudio/zeppelin/tensor – Docker images for Notebook nodes.

All Python scripts, Docker files and other files, which are located in these directories, are OS and CLOUD independent.

OS, CLOUD dependent and common for few templates scripts, functions, files are located in general directory.

    general
    ├───api – all available API
    ├───conf – DLab configuration
    ├───files – OS/Cloud dependent files
    ├───lib – OS/Cloud dependent functions
    ├───scripts – OS/Cloud dependent Python scripts
    └───templates – OS/Cloud dependent templates

These directories may contain differentiation by operating system (Debian/RedHat) or cloud provider (AWS).

Directories of templates (SSN, Edge etc.) contain only scripts, which are OS and CLOUD independent.

If script/function is OS or CLOUD dependent, it should be located in appropriate directory/library in general folder.

The following table describes mostly used scripts:

| Script name/Path             | Description                                                                                      |
|------------------------------|--------------------------------------------------------------------------------------------------|
| Dockerfile                   | Used for building Docker images and represents which Python scripts, templates and other files are needed. Required for each template. |
| base/entrypoint.py           | This file is executed by Docker. It is responsible for setting environment variables, which are passed from Docker and for executing appropriate actions (script in general/api/). |
| base/scripts/\*.py           | Scripts, which are OS independent and are used in each template.                                 |
| general/api/\*.py            | API scripts, which execute appropriate function from fabfile.py.                                 |
| template\_name/fabfile.py    | Is the main file for template and contains all functions, which can be used as template actions. |
| template\_name/scripts/\*.py | Python scripts, which are used for template. They are OS and CLOUD independent.                  |
| general/lib/aws/\*.py        | Contains all functions related to AWS.                                                           |
| general/lib/os/              | This directory is divided by type of OS. All OS dependent functions are located here.            |
| general/lib/os/fab.py        | Contains OS independent functions used for multiple templates.                                   |
| general/scripts/             | Directory is divided by type of Cloud provider and OS.                                           |
| general/scripts/aws/\*.py    | Scripts, which are executed from fabfiles and AWS-specific. The first part of file name defines to which template this script is related to. For example:<br>common\_\*.py – can be executed from more than one template.<br>ssn\_\*.py – are used for SSN template.<br>edge\_\*.py – are used for Edge template. |
| general/scripts/os/\*.py     | Scripts, which are OS independent and can be executed from more than one template. |

#### Docker actions overview

Available Docker images and their actions:

| Docker image                    | Actions                                          |
|---------------------------------|--------------------------------------------------|
| ssn                             | create, terminate                                |
| edge                            | create, terminate, status, start, stop, recreate |
| jupyter/rstudio/zeppelin/tensor | create, terminate, start, stop, configure        |
| emr                             | create, terminate                                |

##### Docker and python execution workflow on example of SSN node

-   Docker command for building images *docker.dlab-base* and *docker.dlab-ssn*:
```
sudo docker build --build-arg OS=debian --build-arg CLOUD=aws --file base/Dockerfile -t docker.dlab-base . ;
sudo docker build --build-arg OS=debian --build-arg CLOUD=aws --file ssn/Dockerfile -t docker.dlab-ssn . ;
```
Example of SSN Docker file:
```
FROM docker.dlab-base:latest
ARG OS
ARG CLOUD
COPY ssn/ /root/
COPY general/scripts/${CLOUD}/ssn_* /root/scripts/
COPY general/lib/os/${OS}/ssn_lib.py /usr/lib/python2.7/dlab/ssn_lib.py
COPY general/files/${CLOUD}/ssn_instance_shapes.lst /root/files/
COPY general/files/${CLOUD}/ssn_policy.json /root/files/
RUN chmod a+x /root/fabfile.py; \
    chmod a+x /root/scripts/*
RUN mkdir /project_tree
COPY . /project_tree
```
Using this Docker file, all required scripts and files will be copied to Docker container.

-   Docker command for building SSN:
```
docker run -i -v /root/KEYNAME.pem:/root/keys/KEYNAME.pem –v /web_app:/root/web_app -e "conf_os_family=debian" -e "conf_os_user=ubuntu" -e "conf_cloud_provider=aws" -e "conf_resource=ssn" -e "aws_ssn_instance_size=t2.medium" -e "aws_region=us-west-2" -e "aws_vpc_id=vpc-111111" -e "aws_subnet_id=subnet-111111" -e "aws_security_groups_ids=sg-11111,sg-22222,sg-33333" -e "conf_key_name=KEYNAME" -e "conf_service_base_name=dlab_test" -e "aws_access_key=Access_Key_ID" -e "aws_secret_access_key=Secret_Access_Key" -e "conf_tag_resource_id=dlab" docker.dlab-ssn --action create ;
```

-   Docker executes *entrypoint.py* script with action *create*. *Entrypoint.py* will set environment variables, which were provided from Docker and execute *general/api/create.py* script:
```
    elif args.action == 'create':
        with hide('running'):
            local("/bin/create.py")
```
-   *general/api/create.py* will execute Fabric command with *run* action:
```
  try:
        local('cd /root; fab run')
```
-   Function *run()* in file *ssn/fabfile.py* will be executed. It will run two scripts *general/scripts/aws/ssn\_prepare.py* and *general/scripts/aws/ssn\_configure.py*:
```
    try:
        local("~/scripts/{}.py".format('ssn_prepare'))
    except Exception as err:
        traceback.print_exc()
        append_result("Failed preparing SSN node. Exception: " + str(err))
        sys.exit(1)

    try:
        local("~/scripts/{}.py".format('ssn_configure'))
    except Exception as err:
        traceback.print_exc()
        append_result("Failed configuring SSN node. Exception: " + str(err))
        sys.exit(1)
```
-   The scripts *general/scripts/aws/ssn\_prepare.py* an *general/scripts/aws/ssn\_configure.py* will execute other Python scripts/functions for:
  1. *ssn\_prepate.py:*
    1.  Creating AWS configuration file
    2. Creating VPC, Subnet, Security Group, IAM roles, Endpoint, Route table, S3 bucket and EC2 instance.
  2. *ssn\_configure.py:*
    1. Installing prerequisites
    2. Installing required packages
    3. Configuring Docker
    4. Configuring DLab Web UI

-   If all scripts/function are executed successfully, Docker container will stop and SSN node will be created.

#### Example of Docker commands

SSN:

```
docker run -i -v <key_path><key_name>.pem:/root/keys/<key_name>.pem -e "aws_region=<region>" -e "conf_service_base_name=<Infrastructure_Tag>" -e  “conf_resource=ssn" -e "aws_access_key=<Access_Key_ID>" -e "aws_secret_access_key=<Secret_Access_Key>" docker.dlab-ssn --action <action>
```
All parameters are listed in section "Self-ServiceNode" chapter.

Other images:
```
docker run -i -v /home/<user>/keys:/root/keys  -v /opt/dlab/tmp/result:/response -v /var/opt/dlab/log/<image>:/logs/<image>  -e <variable1> –e <variable2> docker.dlab-<image> --action <action>
```

#### How to add a new template

First of all, a new directory should be created in *infrastructure-provisioning/src/*.

For example: *infrastructure-provisioning/src/my-tool/*

The following scripts/directories are required to be created in the template directory:

    my-tool
    ├───scripts
    ├───description.json
    ├───Dockerfile
    └───fabfile.py

-   Dockerfile – used for building template Docker image and describes which files, scripts, templates are required and will be copied to template Docker image.
-   fabfile.py – the main script, which contains main functions for this template such as run, stop, terminate, etc.

Here is example of *run*() function for Jupyter Notebook node:

Path: *infrastructure-provisioning/src/jupyter/fabfile.py*
```
def run():
    local_log_filename = "{}_{}_{}.log".format(os.environ['conf_resource'], os.environ['edge_user_name'], os.environ['request_id'])
    local_log_filepath = "/logs/" + os.environ['conf_resource'] + "/" + local_log_filename
    logging.basicConfig(format='%(levelname)-8s [%(asctime)s]  %(message)s',
                        level=logging.DEBUG,
                        filename=local_log_filepath)

    notebook_config = dict()
    notebook_config['uuid'] = str(uuid.uuid4())[:5]

    try:
        params = "--uuid {}".format(notebook_config['uuid'])
        local("~/scripts/{}.py {}".format('common_prepare_notebook', params))
    except Exception as err:
        traceback.print_exc()
        append_result("Failed preparing Notebook node.", str(err))
        sys.exit(1)

    try:
        params = "--uuid {}".format(notebook_config['uuid'])
        local("~/scripts/{}.py {}".format('jupyter_configure', params))
    except Exception as err:
        traceback.print_exc()
        append_result("Failed configuring Notebook node.", str(err))
        sys.exit(1)
```

This function describes process of creating Jupyter node. It is divided into two parts – prepare and configure. Prepare part is common for all notebook templates and responsible for creating of necessary cloud resources, such as EC2 instances, etc. Configure part describes how the appropriate services will be installed.

To configure Jupyter node, the script *jupyter\_configure.py* is executed. This script describes steps for configuring Jupyter node. In each step, the appropriate Python script is executed.

For example:

Path: *infrastructure-provisioning/src/general/scripts/aws/jupyter\_configure.py*
```
    try:
        logging.info('[CONFIGURE JUPYTER NOTEBOOK INSTANCE]')
        print '[CONFIGURE JUPYTER NOTEBOOK INSTANCE]'
        params = "--hostname {} --keyfile {} --region {} --spark_version {} --hadoop_version {} --os_user {} --scala_version {}".\
            format(instance_hostname, keyfile_name, os.environ['aws_region'], os.environ['notebook_spark_version'],
                   os.environ['notebook_hadoop_version'], os.environ['conf_os_user'],
                   os.environ['notebook_scala_version'])
        try:
            local("~/scripts/{}.py {}".format('configure_jupyter_node', params))
```

In this step, the script *infrastructure-provisioning/src/jupyter/scripts/configure\_jupyter\_node.py* will be executed.

Example of script *infrastructure-provisioning/src/jupyter/scripts/configure\_jupyter\_node.py:*
```
if __name__ == "__main__":
    print "Configure connections"
    env['connection_attempts'] = 100
    env.key_filename = [args.keyfile]
    env.host_string = args.os_user + '@' + args.hostname

    print "Configuring notebook server."
    try:
        if not exists('/home/' + args.os_user + '/.ensure_dir'):
            sudo('mkdir /home/' + args.os_user + '/.ensure_dir')
    except:
        sys.exit(1)

    print "Mount additional volume"
    prepare_disk(args.os_user)

    print "Install Java"
    ensure_jre_jdk(args.os_user)
```

This script call functions for configuring Jupyter node. If this function is OS dependent, it will be placed in *infrastructure-provisioning/src/general/lib/\<OS\_family\>/debian/notebook\_lib.py*

All functions in template directory (e.g. *infrastructure-provisioning/src/my-tool/*) should be OS and cloud independent.

All OS or cloud dependent functions should be placed in *infrastructure-provisioning/src/general/lib/* directory.

The following steps are required for each Notebook node:

-   Configure proxy on Notebook instance – the script *infrastructure-provisioning/src/general/scripts/os/notebook\_configure\_proxy.py*
-   Installing user’s key – the script *infrastructure-provisioning/src/base/scripts/install\_user\_key.py*

Other scripts, responsible for configuring Jupyter node are placed in *infrastructure-provisioning/src/jupyter/scripts/*

-   scripts directory – contains all required configuration scripts.
-   descriptsion.json – JSON file for DLab Web UI. In this file you can specify:
  * exploratory\_environment\_shapes – list of EC2 shapes
  * exploratory\_environment\_versions – description of template

Example of this file for Jupyter node:
```
{
  "exploratory_environment_shapes" :
  {
    "For testing" : [
      {"Size": "XS", "Description": "t2.medium", "Type": "t2.medium","Ram": "4 GB","Cpu": "2"}
    ],
    "Memory optimized" : [
      {"Size": "S", "Description": "r3.xlarge", "Type": "r3.xlarge","Ram": "30.5 GB","Cpu": "4"},
      {"Size": "M", "Description": "r3.4xlarge", "Type": "r3.4xlarge","Ram": "122 GB","Cpu": "16"},
      {"Size": "L", "Description": "r3.8xlarge", "Type": "r3.8xlarge","Ram": "244 GB","Cpu": "32"}
    ],
    "GPU optimized": [
      {"Size": "M", "Description": "g2.2xlarge", "Type": "g2.2xlarge","Ram": "15.0 GB","Cpu": "8"}
    ],
    "Compute optimized": [
      {"Size": "S", "Description": "c4.large", "Type": "c4.large","Ram": "3.75 GB","Cpu": "2"},
      {"Size": "M", "Description": "c4.2xlarge", "Type": "c4.2xlarge","Ram": "15.0 GB","Cpu": "8"},
      {"Size": "L", "Description": "c4.8xlarge", "Type": "c4.8xlarge","Ram": "60.0 GB","Cpu": "36"}
    ]
  },
  "exploratory_environment_versions" :
  [
    {
      "template_name": "Jupyter 1.5",
      "description": "Base image with jupyter node creation routines",
      "environment_type": "exploratory",
      "version": "jupyter-1.6",
      "vendor": "AWS"
    }
  ]
}
```

Additionally, following directories could be created:

-   templates – directory for new templates;

-   files – directory for files used by newly added templates only;

All Docker images are being built while creating SSN node. To add newly created template, add it to the list of images in the
following script:

Path: *infrastructure-provisioning/src/general/scripts/aws/ssn\_configure.py*
```
    try:
        logging.info('[CONFIGURING DOCKER AT SSN INSTANCE]')
        print('[CONFIGURING DOCKER AT SSN INSTANCE]')
        additional_config = [{"name": "base", "tag": "latest"},
                             {"name": "edge", "tag": "latest"},
                             {"name": "jupyter", "tag": "latest"},
                             {"name": "rstudio", "tag": "latest"},
                             {"name": "zeppelin", "tag": "latest"},
                             {"name": "tensor", "tag": "latest"},
                             {"name": "emr", "tag": "latest"}]
```
For example:
```
...

   {"name": "my-tool", "tag": "latest"},

...
```