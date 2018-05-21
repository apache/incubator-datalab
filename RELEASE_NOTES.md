# DLab is Self-service, Fail-safe Exploratory Environment for Collaborative Data Science Workflow

## New features in v1.9

- Implemented ssh key pair generation for user
- Added ability to stop Data Engine with further start
- Added possibility to install additional packages for RStudio from console
- Added possibility to stop or terminate user environment by administrator
- Implemented restriction of available instance shapes for notebooks based on user roles

## Improvements in v1.9

- Added possibility to install Python packages for RStudio
- Implemented a scheduler for notebook (added UI) and Data Engine 
- Implemented creation images from existing notebook including libraries (added UI)
- Updated User guide


## Bug fixes in v1.9

- Sparklyr (r package) and tkinter  (yum) libraries are successfully installed on RStudio
- User key is removed from MongoDB after Edge termination by administrator
- Edge terminate process kills only Data Engine Service of related user not all users on AWS
- Available lib list is successfully obtained for Data Engine Service on AWS


## Known issues in v1.9

- Remote kernel list for Data Engine is not updated after stop/start
- Storage permissions aren't differentiated by users thought Dataproc permissions (users can create clusters, but have R/W access to all buckets)
- DeepLearning, TensorFlow templates are not supported on Microsoft Azure for Red Hat Enterprise Linux
- Microsoft Azure resource name length should not exceed 80 chars
- Billing is not available on Microsoft Azure
- GCP resource name length should not exceed 64 chars
- Low priority Virtual Machines are not supported yet on Microsoft Azure
- Zeppelin supports only Dataproc 1.1 on GCP
- Rare problem during notebook stopping/starting and creating clusters (impossible to automatically determine credentials (GCP))


  
**NOTE:** DLab has not been tested on GCP for Red Hat Enterprise Linux



