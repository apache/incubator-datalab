# DLab is Self-service, Fail-safe Exploratory Environment for Collaborative Data Science Workflow
# DLab is Self-service, Fail-safe Exploratory Environment for Collaborative Data Science Workflow

## New features in v2.2
**All Cloud platforms:**
- added concept of Projects into DLab. Now users can unite under Projects and collaborate
- for ease of use we've added web terminal for all DLab Notebooks
- updated versions of installed software:
	* angular 8.2.7

**GCP:**
- added billing report to monitor Cloud resources usage into DLab, including ability to manage billing quotas
- updated versions of installed software:
	* Dataproc 1.3

## Improvements in v2.2
**All Cloud platforms:**
- implemented login via KeyCloak to support integration with multiple SAML and OAUTH2 identity providers
- added DLab version into WebUI
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
- DLab deployment  is unavailable if Data Lake is enabled
- custom image creation from Notebook fails and deletes existed Notebook

**Refer to the following link in order to view the other major/minor issues in v2.2:**

[Apache DLab: known issues](https://issues.apache.org/jira/issues/?filter=12347602 "Apache DLab: known issues")

## Known issues caused by cloud provider limitations in v2.2
**Microsoft Azure:**
- resource name length should not exceed 80 chars
- TensorFlow templates are not supported for Red Hat Enterprise Linux
- low priority Virtual Machines are not supported yet

**GCP:**
- resource name length should not exceed 64 chars
- billing data is not available
- **NOTE:** DLab has not been tested on GCP for Red Hat Enterprise Linux

## New features in v2.1
**All Cloud platforms:**
- implemented tuning Apache Spark standalone cluster and local spark  configurations from WEB UI (except for Apache Zeppelin)
- added a reminder after user logged in notifying that corresponding resources are about to be stopped/terminated
- implemented SSN load monitor: CPU, Memory, HDD
- updated versions of installed software:
    * Jupyter 5.7.4
    * RStudio 1.1.463
    * Apache Zeppelin 0.8.0
    * Apache Spark 2.3.2 for standalone cluster 
    * Scala 2.12.8
    * CNTK 2.3.1
    * Keras 2.1.6 (except for DeepLearning - 2.0.8)
    * MXNET 1.3.1
    * Theano 1.0.3
    * ungit 1.4.36

**AWS:**
- implemented tuning Data Engine Service from WEB UI (except for Apache Zeppelin)
- added support of new version of Data Engine Service (AWS EMR) 5.19.0

**MS azure and AWS:**
- implemented ability to manage total billing quota for DLab as well as billing quota per user

## Improvements in v2.1

**All Cloud platforms:**
- added ability to configure instance size/shape (CPU, RAM) from DLab UI for different user groups
- added possibility to install Java dependencies from DLab UI
- added alternative way to access analytical notebooks just by clicking on notebook's direct URL.
    * added LDAP authorization in Squid (user should provide his LDAP credentials when accessing notebooks/Data Engine/Data Engine Service via browser)
- improved error handling for various scenarios on UI side 
- added support of installing DLab into two VPCs

**MS Azure:**
- it is now possible to install DLab only with private IP’s 

## Bug fixes in v2.1
**AWS:**
- fixed pricing retrieval logic to optimize RAM usage on SSN for small instances

## Known issues in v2.1
**All Cloud platforms:**
- remote kernel list for Data Engine is not updated after stop/start Data Engine 
- following links can be opened via tunnel for Data Engine/Data Engine: service: worker/application ID, application detail UI, event timeline, logs for Data Engine
- if Apache Zeppelin is created from AMI with different instance shape, spark memory size is the same as in created AMI.
- sparklyr library (r package) can not be installed on RStudio, RStudio with TensorFlow notebooks
- Spark default configuration for Apache Zeppelin can not be changed from DLab UI.  Currently it can be done directly through Apache Zeppelin interpreter menu.
For more details please refer for Apache Zeppelin official documentation: https://zeppelin.apache.org/docs/0.8.0/usage/interpreter/overview.html
- shell interpreter for Apache Zeppelin is missed for some instance shapes 
- executor memory is not allocated depending on notebook instance shape for local spark


**AWS**
- can not open master application URL on resource manager page, issue known for Data Engine Service v.5.12.0
- java library installation fails on DLab UI on Data Engine Service in case when it is installed together with libraries from other groups.

**GCP:**
- storage permissions aren't differentiated by users via Dataproc permissions (all users have R/W access to other users buckets)
- Data Engine Service creation is failing after environment has been recreated
- It is temporarily not possible to run playbooks using remote kernel of Data Engine (dependencies issue)
- DeepLearning creation fails 

**Microsoft Azure:**
- creation of Zeppelin from custom image fails on the step when cluster kernels are removing
- start Notebook by scheduler does not work when Data Lake is enabled
- playbook running on Apache Zeppelin fails due to impossible connection to blob via wasbs protocol 

## Known issues caused by cloud provider limitations in v2.1

**Microsoft Azure:**
- resource name length should not exceed 80 chars
- TensorFlow templates are not supported for Red Hat Enterprise Linux
- low priority Virtual Machines are not supported yet
- occasionally billing data is not available for Notebook secondary disk

**GCP:**
- resource name length should not exceed 64 chars
- billing data is not available
- **NOTE:** DLab has not been tested on GCP for Red Hat Enterprise Linux