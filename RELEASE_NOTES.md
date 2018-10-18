# DLab is Self-service, Fail-safe Exploratory Environment for Collaborative Data Science Workflow

## Bug fixes in v2.0.2
- fixed Apache Toree installation for Jupyter notebook 


## Known issues in v2.0.2
**All Cloud platforms:**
- remote kernel list for Data Engine is not updated after stop/start Data Engine
- following links can be opened via tunnel for Data Engine/Data Engine: service: worker/application ID, application detail UI, event timeline, logs for Data Engine

**AWS**
- can not open master application URL on resource manager page, issue known for Data Engine Service v.5.12.0

**GCP:**
- storage permissions aren't differentiated by users via Dataproc permissions (all users have R/W access to other users buckets)
- Data Engine Service creation is failing after environment has been recreated
- it is temporarily not possible to run playbooks using remote kernel of Data Engine (dependencies issue)
- DeepLearning creation fails 

**Microsoft Azure:**
- creation of Zeppelin from custom image fails on the step when cluster kernels are removing
- start Notebook by scheduler does not work when Data Lake is enabled 

## Known issues caused by cloud provider limitations in v2.0.2

**Microsoft Azure:**
- resource name length should not exceed 80 chars
- TensorFlow templates are not supported for Red Hat Enterprise Linux
- low priority Virtual Machines are not supported yet
- occasionally billing data is not available for Notebook secondary disk

**GCP:**
- resource name length should not exceed 64 chars
- billing data is not available
- **NOTE:** DLab has not been tested on GCP for Red Hat Enterprise Linux