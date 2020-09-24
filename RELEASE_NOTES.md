# Data Lab is Self-service, Fail-safe Exploratory Environment for Collaborative Data Science Workflow

## New features in v2.2
**All Cloud platforms:**
- added concept of Projects into Data Lab. Now users can unite under Projects and collaborate
- for ease of use we've added web terminal for all Data Lab Notebooks
- updated versions of installed software:
	* angular 8.2.7

**GCP:**
- added billing report to monitor Cloud resources usage into Data Lab, including ability to manage billing quotas
- updated versions of installed software:
	* Dataproc 1.3

## Improvements in v2.2
**All Cloud platforms:**
- implemented login via KeyCloak to support integration with multiple SAML and OAUTH2 identity providers
- added Data Lab version into WebUI
- augmented ‘Environment management’ page
- added possibility to tag Notebook from UI
- added possibility to terminate computational resources via scheduler

**GCP:**
- added possibility to create Notebook/Data Engine from an AMI image

**AWS and GCP:**
- UnGit tool now allows working with remote repositories over ssh
- implemented possibility to view Data Engine Service version on UI after creation

## Bug fixes in v2.2
**All Cloud platforms:**
- fixed  sparklyr library (r package) installation on RStudio, RStudio with TensorFlow notebooks

**GCP:**
- fixed a bug when Data Engine creation fails for DeepLearning template
- fixed a bug when Jupyter does not start successfully after Data Engine Service creation (create Jupyter -> create Data Engine -> stop Jupyter -> Jupyter fails)
- fixed a bug when DeepLearning creation was failing

## Known issues in v2.2
**All Cloud platforms:**
- Notebook name should be unique per project for different users in another case it is impossible to operate Notebook with the same name after the first instance creation

**Microsoft Azure:**
- Data Lab deployment  is unavailable if Data Lake is enabled
- custom image creation from Notebook fails and deletes existed Notebook

**Refer to the following link in order to view the other major/minor issues in v2.2:**

[Apache Data Lab: known issues](https://issues.apache.org/jira/issues/?filter=12347602 "Apache Data Lab: known issues")

## Known issues caused by cloud provider limitations in v2.2
**Microsoft Azure:**
- resource name length should not exceed 80 chars
- TensorFlow templates are not supported for Red Hat Enterprise Linux
- low priority Virtual Machines are not supported yet

**GCP:**
- resource name length should not exceed 64 chars
- billing data is not available
- **NOTE:** Data Lab has not been tested on GCP for Red Hat Enterprise Linux
