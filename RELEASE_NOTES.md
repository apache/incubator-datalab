# DLab is Self-service, Fail-safe Exploratory Environment for Collaborative Data Science Workflow

## New features in v1.5

- Introduced inital version of collaboration environemnt: 
  - Web UI credential manager functionality is added to manage access to Git repositories
  - Added support of a shared storage on S3
  - UnGit, an easy-to-use tool to use Git is now deployed on every analytical tool
- Added functionality to manage Notebook servers libraries and dependencies via Web UI 
- Added billing report functionality
- Added support of new version of TensorFlow v.1.1.0
- Added support of new version of Rstudio v1.0.143

## Improvements in v.1.5

- DLab can now be deployed into AWS China region
- Jupyter notebooks are now automatically adjusted to automatically fit to browser width
- Added handling of cases when EMR was manually deleted on AWS, but still was showing as Running in DLab
- Added support of EMR 5.6.0
- Added scroller to in-grid billing functionality
- Size of private subnet is now configurable

## Bug fixes in v.1.5

- Fixed a problem with Caffe2 not working on DeepLearning template
- Fix a problem with environment up time counters showing wrong data for EMR
- Removed LDAP server credentials from security.yml
- Fixed a problem when Mongo DB couldn't be started after stopping/restarting SSN on RedHat
- DLab now checks for EMR default roles and doesn't create them if they exist
- Fixed a problem with cached security token in Dlab
- Small UI enhancements for in-grid billing popup
