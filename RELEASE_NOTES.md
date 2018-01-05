# DLab is Self-service, Fail-safe Exploratory Environment for Collaborative Data Science Workflow

## New features in v1.8

- OAuth2 authentication and authorization on Microsoft Azure
- Added support of Data Lake Store on Microsoft Azure
- Integration with  Google Cloud Platform (by means of deployment scripts, no UI yet)

## Improvements in v1.8

- More readable error information for libraries installation
- Added type identifier for computational resources during libraries installation, You can now distinguish which libraries have been installed on a notebooks and which on cluster 
- Immediate validation on UI if cluster names are not duplicated


## Bug fixes in v1.8

Logout if user session is inactive for configured period of time 
Occasional problem with DLab not successful login from the first attempt on Microsoft Azure
Libraries from terminated clusters are shown on UI


## Known issues in v1.8

- DeepLearning, TensorFlow templates are not supported on Microsoft Azure for Red Hat Enterprise Linux
- Microsoft Azure resource name length should not exceed 80 chars
- GCP resource name length should not exceed 64 chars
- Spark cluster status is not updated from “terminating” after notebook stop action
- Low priority Virtual Machines are not supported yet on Microsoft Azure


  
**NOTE:** DLab has not been tested on GCP for Red Hat Enterprise Linux



