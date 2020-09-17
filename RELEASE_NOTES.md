# DLab is Self-service, Fail-safe Exploratory Environment for Collaborative Data Science Workflow

## New features in v2.4.0
**All Cloud platforms:**
- Implemented bucket browser. Now user is able to manage Cloud data source by means of accessing Cloud Blob Storage from DLab Web UI;
- Added support of audit. Now DLab administrators can view history of all actions;
- Updated versions of installed software:
  * Ubuntu v.18.04;
  * TensorFlow notebook v.2.1.0;
  * MongoDB v.4.2.

**AWS:**
- Added support of new version of Data Engine Service (EMR) v.5.30.0 and v.6.0.0.

## Improvements in v2.4.0
**All Cloud platforms:**
- Added support of connection via Livy and SparkMagic for Jupyter and RStudio notebooks;
- Added ability to select multiple resources on &#39;Environment management&#39; to make user experience easier and more intuitive;
- Added support to install libraries of particular version from DLab Web UI. Also, now user is able to update/downgrade library via Web UI;
- Extended billing functionality introducing new entity - monthly project quota(s);
- Added notifications for cases when project quota is exceeded;
- Conveyed analytical environment URL&#39;s to DLab administration page.

**GCP:**
- Added possibility to create custom image for notebook.

## Bug fixes in v2.4.0
**All Cloud platforms:**
- Fixed a bug when administrative permissions disappeared after endpoint connectivity issues;
- Fixed a bug when all resources disappeared in &#39;List of resources&#39; page after endpoint connectivity issues;
- Fixed a bug when administrative role could not be edited for already existing group;
- Fixed a bug when billing report was not populated in Safari;
- Fixed a bug with discrepancies in detailed billing and in-grid billing report.

**GCP:**
- Fixed a bug when billing was not correctly updated for period overlapping two calendar years;

**Microsoft Azure:**
- Fixed a rare bug when notebooks or SSN were not always created successfully from the first attempt.

## Known issues in v2.4.0
**GCP:**
- SSO is not available for Superset.

**Microsoft Azure:**
- Notebook creation fails on RedHat;
- Web terminal is not working for Notebooks only for remote endpoint.

*Refer to the following link in order to view the other major/minor issues in v2.4.0:*

[Apache DLab: Known issues](https://issues.apache.org/jira/issues/?filter=12349399 "Apache DLab: Known issues")

## Known issues caused by cloud provider limitations in v2.4.0

**Microsoft Azure:**
- Resource name length should not exceed 80 chars;
- TensorFlow templates are not supported for RedHat Enterprise Linux;
- Low priority Virtual Machines are not supported yet.

**GCP:**
- Resource name length should not exceed 64 chars;
- NOTE: DLab has not been tested on GCP for RedHat Enterprise Linux.
