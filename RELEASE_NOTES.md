# DLab is Self-service, Fail-safe Exploratory Environment for Collaborative Data Science Workflow

## New features in v2.0
**All Cloud platforms:**
- added possibility to go to links (Notebook UI, ungit, Tensorboard) without opening tunnel to Edge on AWS
- implemented environment management page
- added possibility to generate a key during key reuploading

**AWS:**
- added new "RStudio with TensorFlow" template on AWS
- Data Engine/Data Engine Service job tracker URL is displayed on Web UI on AWS
- added possibility to use AWS default reporting as a source for DLAB billing


## Improvements in v2.0

**All Cloud platforms:**
- it is now possible to separately configure start and stop schedules for analytical resources
- added shell interpreter for Zeppelin

**AWS:**
- optimized starting/stopping duration of Data Engine service

**MS Azure and AWS:**
- DLab's billing report, now indicates costs associated with any mounted storage of analytical tool


## Bug fixes in v2.0
**AWS:**
- when computational resource name is part of a name of any other computational resource - it will correspondingly affected during stop/terminate actions (e.g. stopping EMR1 will stop EMR11, terminating EMR1 will terminate EMR11)

**GCP:**
- fixed occasionally reproducible problem: failure in Notebook stopping/starting and failure in creation of computational resources (it was not possible to automatically determine credentials)


## Known issues in v2.0
**All Cloud platforms:**
- remote kernel list for Data Engine is not updated after stop/start Data Engine

**GCP:**
- storage permissions aren't differentiated by users via Dataproc permissions (all users have R/W access to other users buckets)
- data engine service creation is failing after environment has been recreated

**Microsoft Azure:**
- creation of Zeppelin from custom image fails on the step when cluster kernels are removing
- start Notebook by scheduler does not work when Data Lake is enabled 

## Known issues caused by cloud provider limitations in v2.0

**Microsoft Azure:**
- resource name length should not exceed 80 chars
- TensorFlow templates are not supported for Red Hat Enterprise Linux
- low priority Virtual Machines are not supported yet
- occasionally billing data is not available for Notebook secondary disk

**GCP:**
- resource name length should not exceed 64 chars
- billing data is not available
- **NOTE:** DLab has not been tested on GCP for Red Hat Enterprise Linux