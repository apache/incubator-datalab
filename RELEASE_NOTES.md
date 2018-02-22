# DLab is Self-service, Fail-safe Exploratory Environment for Collaborative Data Science Workflow

## New features in v1.9_alpha
- Added full support of Google Cloud Platform on UI side (except billing)
- Implemented Backup functionality on UI side
- Updated UI to Angular version 5
- Updated versions of software:
   - MXNET 1.0.0
   - RStudio 1.1.383
   - Jupyter 5.2.0
   - TensorFlow 1.4.0
   - Hadoop 3.0 (for MS Azure Data Lake)

## Improvements in v1.9_alpha
 - Improved custom icons for menu items
 - Implemented a scheduler for Notebooks management (Back-end implementation only, UI to be added in next release)
 - Implemented creation images from existing notebook including libraries (BE only, no UI yet)
 - Added possibility to create notebook from preconfigured image (BE only)
 - Increased time expiration of ssl certificate for java keystore
 - Updated User guide and Technical guide

## Bug fixes in v1.9-alpha
 - Improved handling of environment status messages updates in rare cases

## Known issues in v1.9-alpha
 - Storage permissions aren't differentiated by users thought Dataproc permissions (users can create clusters, but have R/W access to all buckets)
 - DeepLearning, TensorFlow templates are not supported on Microsoft Azure for Red Hat Enterprise Linux
 - Microsoft Azure resource name length should not exceed 80 chars
 - GCP resource name length should not exceed 64 chars
 - Low priority Virtual Machines are not supported yet on Microsoft Azure
 - Zeppelin supports only Dataproc 1.1 on GCP
 - Rare problem during notebook/cluster stop/start (impossible to automatically determine credentials (GCP))



**NOTE:** DLab has not been tested on GCP for Red Hat Enterprise Linux


