# DataLab is Self-service, Fail-safe Exploratory Environment for Collaborative Data Science Workflow

## Improvements in v2.5.1
**All cloud platforms:**
- Added GPU filter type and count for easier environment management by administrator;
- Added explanation for open terminal action on Audit page.

## Bug fixes in v2.5.1
**Azure:**
- Fixed a bug when instance creation failed on stage of devtools installation;
- Fixed a bug when Apache Zeppelin notebook creation failed during shell interpreter configuration;
- Fixed a bug when edge node status on WEB DataLab UI was not synced up with Cloud instance status;
- Fixed a bug when DeepLearning creation failed due to wrong path to connector;
- Fixed a bug when not all billing drop down values were visible;
- Fixed minor  UI issues which were reproduced only for smaller desktop size;
- Fixed a bug when connection for Jupyter R/Scala kernels were unsuccessful;
- Fixed a bug when Data Engine creation failed on Jupyter/RStudio/Apache Zeppelin notebooks;
- Fixed a bug when very often notebook/Data Engine creation and stopping failed due to low level socket;
- Fixed a bug when Jupyter/RStudio/DeepLearning notebooks creation failed from image;
- Fixed a bug when SSN/any type of notebook creation was not always successful from the first attempt.

**Azure and GCP:**
- Fixed a bug when time to time DeepLearning notebook creation failed on stage of nvidia installation;
- Fixed a bug when sometimes any type of notebook creation failed during disk mount.

## Known issues in v2.5.1
**GCP:**
- Superset creation fails during configuration;
- SSO is not available for Superset.

**Microsoft Azure:**
- Notebook WEB terminal does not work for remote endpoint.

*Refer to the following link in order to view other major/minor issues in v2.5.1*

[Apache DataLab: Known issues](https://issues.apache.org/jira/issues/?filter=12351099 "Apache DataLab: Known issues")

## Known issues caused by cloud provider limitations in v2.5.1

**Microsoft Azure:**
- Resource name length should not exceed 80 chars.

**GCP:**
- Resource name length should not exceed 64 chars.

**NOTE:** the DataLab has not been tested for RedHat Enterprise Linux.
