# DLab is Self-service, Fail-safe Exploratory Environment for Collaborative Data Science Workflow

## New features in v1.9

- Implemented automatic ssh key pair generation for user
- Added ability to stop Data Engine based computational resources
- Implemented packages installation from RStudio console
- DLab administrator can now stop or terminate particular users environment
- Usage of certain instance shapes for Notebooks can be configured for per user group

## Improvements in v1.9

- Added possibility to install Python packages for RStudio
- Implemented scheduler for Notebooks and Data Engines based computational resources that allows to automatically start/stop user resources 
- It is not possible to create images out of existing Notebooks, which reduces time to configure new Notebooks (by reusing images with previously installed libraries and packages)


## Bug fixes in v1.9

- Fixed a bug that prevented to install Sparklyr (r package) and tkinter  (yum) libraries on RStudio
- Fixed a bug that prevented users to upload a key to Edge node after Edge node has been terminated by administrator
- Fixed a bug when terminating environment of particular user, Data Engine Services computational resources were also terminated for other users on AWS
- Fixed a bug when available lib list was not retrieved for Data Engine Service based computational resources on AWS


## Known issues in v1.9

- Remote kernel list for Data Engine is not updated after stop/start Data Engine
- On GCP storage permissions aren't differentiated by users via Dataproc permissions (all users have R/W access to other users buckets)
- Data engine service creation is not successful after environment has been recreated on GCP
- Rare problem during Notebook stopping/starting and creating clusters ( not possible to automatically determine credentials (GCP))
- Occasional problem with not successful SSN creation on Microsoft Azure and Google Cloud Platform
- Rarely Notebook creation is not successful because of unabling to resolve the host on Microsoft Azure
- Creation of Zeppelin from custom image fails on the step when cluster kernels are removing on Microsoft Azure
- Start Notebook by scheduler does not work on Microsoft Azure when Data Lake enabled


## Known issues caused by cloud provider limitations in v1.9

- Microsoft Azure resource name length should not exceed 80 chars
- GCP resource name length should not exceed 64 chars
- DeepLearning, TensorFlow templates are not supported on Microsoft Azure for Red Hat Enterprise Linux
- Low priority Virtual Machines are not supported yet on Microsoft Azure
- Billing is not available on GCP
  
**NOTE:** DLab has not been tested on GCP for Red Hat Enterprise Linux



