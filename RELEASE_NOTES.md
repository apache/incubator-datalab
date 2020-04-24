#
# DLab is Self-service, Fail-safe Exploratory Environment for Collaborative Data Science Workflow

## New features in v2.3

**All Cloud platforms:**
- Added support for multi-Cloud orchestration for AWS, Azure and GCP. Now, a single DLab instance can connect to the above Clouds, by means of respective set of API&#39;s, deployed on cloud endpoints;
- Added JupyterLab v.0.35.6 template
- Updated versions of installed software:
  - Jupyter notebook v.6.0.2;
  - Apache Zeppelin v.0.8.2;
  - RStudio v.1.2.5033;
  - Apache Spark v.2.4.4 for standalone cluster;

**AWS:**
- Added support of new version of Data Engine Service (EMR) v.5.28.0;

**GCP:**
- Added support of new version of Data Engine Service (Dataproc) v.1.4;
- Added new template Superset v.0.35.1;

## Improvements in v2.3
**All Cloud platforms:**
- Grouped project management actions in single Edit project menu for ease of use;
- Introduced new &quot;project admin&quot; role;
- SSO now also works for Notebooks;
- Implemented ability to filter installed libraries;
- Added possibility to sort by project/user/charges in &#39;Billing report&#39; page;
- Added test option for remote endpoint;

## Bug fixes in v2.3
**All Cloud platforms:**
- Fixed a bug when Notebook name should be unique per project for different users, since it was impossible to operate Notebook with the same name after the first instance creation;
- Fixed a bug when administrator could not stop/terminate Notebook/computational resources created by another user;
- Fixed a bug when shell interpreter was not showing up for Apache Zeppelin;
- Fixed a bug when scheduler by start time was not triggered for Data Engine;
- Fixed a bug when it was possible to start Notebook if project quota was exceeded;
- Fixed a bug when scheduler for stopping was not triggered after total quota depletion;

**AWS:**
- Fixed a bug when Notebook image/snapshot were still available after SSN termination;

**Microsoft Azure:**
- Fixed a bug when custom image creation from Notebook failed and deleted the existing Notebook of another user;
- Fixed a bug when detailed billing was not available;
- Fixed a bug when spark reconfiguration failed on Data Engine;
- Fixed a bug when billing data was not available after calendar filter usage;

## Known issues in v2.3
**GCP:**
- SSO is not available for Superset;

**Microsoft Azure:**
- Notebook creation fails on RedHat;
- Web terminal is not working for Notebooks only for remote endpoint;

Refer to the following link in order to view the other major/minor issues in v2.3:

[Apache DLab: known issues](https://issues.apache.org/jira/issues/?filter=12348876#](https://issues.apache.org/jira/issues/?filter=12348876 "Apache DLab: known issues")

## Known issues caused by cloud provider limitations in v2.3
**Microsoft Azure:**
- Resource name length should not exceed 80 chars;
- TensorFlow templates are not supported for RedHat Enterprise Linux;
- Low priority Virtual Machines are not supported yet;

**GCP:**
- Resource name length should not exceed 64 chars;
- NOTE: DLab has not been tested on GCP for RedHat Enterprise Linux;
