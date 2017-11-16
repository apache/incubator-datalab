# DLab is Self-service, Fail-safe Exploratory Environment for Collaborative Data Science Workflow

## New features in v1.7

- Added support of distributed computation for GPU-enabled analytical tools (TensorFlow and DeepLearning)
- Implemented common AMI functionality. Now all analytical tools for all users are created out of single AMI
- Added support of Spark Data Engine for AWS
- Added ability to manage libraries for Spark Cluster (AWS and Microsoft Azure) and EMR (AWS)
- Added detailed billing report and in-grid billing report functionality for Microsoft Azure
- Updated http://dlab.opensource.epam.com

## Improvements in v.1.7

- Changed layout of Manage Libraries modal dialog component
- Improved error handling for libraries installation
- Added support of p2 instances for GPU templates
- Stability improvements for UnGit

## Bug fixes in v.1.7

- Data Engine slaves are not terminated when notebook is stopped or Data Engine creation fails
- Issue with matplotlib plot backend WebAgg on Zeppelin
- Termination of Spark cluster triggers termination of other Spark clusters on the same notebook
- Notebook proxy host is not correct if notebook is created out of existing AMI image
- Remote kernels (Data Engine/Data Engine Service) are not cleared out in RStudio if notebook server is stopped or Data Engine/Data Engine Service is terminated
- libjpeg library is not found when CNTK is running on DeepLearning notebook on AWS for Red Hat Enterprise Linux
- Minor corrections of tool-tip for lib \'Retry\' icon in Microsoft Edge browser

## Known issues in v1.7

- DLab does not support DeepLearning and TensorFlow templates on Microsoft Azure for Red Hat Enterprise Linux
- Total number of characters in MS Azure instance name should not exceed 80 chars
- Low priority Virtual Machines are not supported yet on Microsoft Azure
- Sometimes login DLab isn't  successful from the first attempt on Microsoft Azure infrastructure
