# DataLab is Self-service, Fail-safe Exploratory Environment for Collaborative Data Science Workflow

## New features in v2.5.0
**All Cloud platforms:**
- Implemented Configuration page. Now DataLab administrators can view, edit configuration files and restart the services;
- Implemented localization. Now the DataLab UI is automatically updated to a specific location (e.g., date and time format, currency view etc.);
- Updated versions of installed software:
  * Ubuntu v.20.04;
  * Python v.3.x;
  * Pip v.21.0.1;
  * R v.4.1.0;
  * Angular v.10.2.2;
  * Jupyter notebook v.6.1.6;
  * RStudio notebook v.1.4.1103;
  * Apache Zeppelin notebook v.0.9.0;
  * TensorFlow notebook v.2.5.0;
  * Apache Spark v.3.0.1;
  * Ungit v.1.5.15.

**AWS:**
- Added support of new version of Data Engine Service (EMR) v.6.2.0.

**GCP:**
- Added support of new version of Data Engine Service (Dataproc) v.2.0.0-RC22-ubuntu18.

## Improvements in v2.5.0
**All Cloud platforms:**
- Added DeepLearning notebook creation based on Cloud native image;
- Added specific python versions via virtual environments for all notebooks and compute resources (except Data Engine Service and DeepLearning).

**GCP:**
- Added optional notebook/compute creation with GPU for Jupyter notebook, Data Engine Service (Dataproc) and Data Engine (standalone cluster).

## Bug fixes in v2.5.0
**All Cloud platforms:**
- Fixed a bug when instance status on the DataLab WEB UI was not synched up with Cloud instance status after provisioning restart;
- Fixed a bug when Spark executor memory was not allocated in depends on notebook instance shape;
- Fixed a bug when reminder about notebook stopping continued to show up after scheduler had been triggered;
- Fixed a bug when library status did not change on the DataLab WEB UI in case of unknown library name installation.

## Known issues in v2.5.0
**GCP:**
- Superset creation fails during configuration;
- SSO is not available for Superset.

**Microsoft Azure:**
- Notebook WEB terminal does not work for remote endpoint.

*Refer to the following link in order to view other major/minor issues in v2.5.0:*

[Apache DataLab: Known issues](https://issues.apache.org/jira/issues/?filter=12350724 "Apache DataLab: Known issues")

## Known issues caused by cloud provider limitations in v2.5.0

**Microsoft Azure:**
- Resource name length should not exceed 80 chars;

**GCP:**
- Resource name length should not exceed 64 chars;

**NOTE:** the DataLab has not been tested on GCP for RedHat Enterprise Linux.
