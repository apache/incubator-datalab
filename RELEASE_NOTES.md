# DataLab is Self-service, Fail-safe Exploratory Environment for Collaborative Data Science Workflow

## New features in v2.6.0

**All cloud platforms:**
- Implemented Images page, where DataLab users may view and manage custom images. 
  * Now DataLab user can: 
    - View the list of own and shared custom images; 
    - View additional info about custom image; 
    - View activities on sharing/stopping sharing custom images; 
    - Share custom images with selected users or user groups; 
    - Stop sharing custom images with selected users or user groups; 
    - Terminate custom images. 
  * DataLab administrators can grant permission to user for: 
    - Sharing own custom images; 
    - Terminating own custom images; 
    - Notebook creation based on own custom images; 
    - Notebook creation based on shared custom images.
- Updated versions of installed software:
  * Jupyter notebook v.6.4.12;
  * JupyterLab notebook v.3.4.3;
  * Superset notebook v.1.5.1;
  * TensorFlow notebook v.2.9.1;
  * RStudio notebook v.2022.02.2-485;
  * Angular v.11.2.14;
  * Keycloak v.18.0.1.
- Added the possibility to connect a new data platform to DataLab account.
  * Now DataLab users can connect MLflow platform.

**AWS:**
- Added a new template JupyterLab with TensorFlow.

**Azure:**
- Added support of Data Engine Service (HDInsight) for RStudio, Jupyter & Apache Zeppelin notebooks.

## Improvements in v2.6.0

**All cloud platforms:**
- Added ability to start user's notebook from administrative panel;
- Improved filter function by creating a separate “Filter” action button (in this release available only for Image page, to be updated on other pages in the next releases);
- Added minor improvements for About & Help sections to make user experience easier and more intuitive.

## Bug fixes in v2.6.0

**All cloud platforms:**
- Fixed a bug when project creation was allowed after total quota exceeded;
- Fixed a bug when Ungit link leaded to 502 error for DeepLearning notebook;
- Fixed wrong date & time data of uploaded objects in the bucket browser;
- Fixed a bug when R package was absent for installation from DataLab WEB UI for RStudio & Apache Zeppelin notebooks;

**AWS:**
- Fixed a bug when EMR creation failed on RStudio & Apache Zeppelin notebooks.

**GCP:**
- Fixed a bug when Superset creation failed during configuration;
- Fixed a bug when  Dataproc creation failed on RStudio & Apache Zeppelin notebooks;
- Fixed a bug when billing data were absent for Compute;
- Fixed a bug when DataLab deployment was unsuccessful in existing VPC or subnet.

## Known issues in v2.6.0

**GCP:**
- SSO is not available for Superset.

*Refer to the following link in order to view other major/minor issues in v2.6.0*

[Apache DataLab: Known issues](https://issues.apache.org/jira/issues/?filter=12352236)

## Known issues caused by cloud provider limitations in v2.6.0

**Microsoft Azure:**
- Resource name length should not exceed 80 chars.

**GCP:**
- Resource name length should not exceed 64 chars.

**NOTE:** the DataLab has not been tested for RedHat Enterprise Linux.
