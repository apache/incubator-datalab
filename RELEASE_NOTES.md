# DLab is Self-service, Fail-safe Exploratory Environment for Collaborative Data Science Workflow

## New features in v1.6

- DLab can now be deployed on Microsoft Azure platform
- Added support of Zeppelin 0.7.2
- Added support of TensorFlow 1.3.0
- Added a possibility to export billing report into .csv
- Added support of Spark 2.1.0 for local kernels of all analytical tools

## Improvements in v.1.6

- Improved error handling for installation of additional libraries
- Released DLab promotion website: http://dlab.opensource.epam.com

## Bug fixes in v.1.6

- Fixed a problem when additional disk was not mounted during DeepLearning provisioning
- Fixed Caffe compiling in DeepLearning template on RedHat platform
- Fixed a problem when Caffe2 not working on DeepLearning template
- Fixed a bug when Jupyter wasn’t created on RedHat in rare cases
- Fixed an issue when one could start notebook server instance when its status was Stopping
- Fixed a bug when LDAP roles cache prevented users from being logged in
- Fixed a problem with remote kernels not being cleared out in Jupyter, if linked EMR was terminated
- Fixed a problem with remote kernels not being cleared out in RStudio if notebook server was stopped
- Fixed an issue with local Zeppelin kernel connectivity with AWS S3 bucket
- Fixed problems with backup and restore scripts
- Fixed broken calendar grid on "Reporting" page in MS Edge browser

## Known issues in v1.6

- AMI files are not created for analytical templates on Microsoft Azure
- DLab does not support DeepLearning and TensorFlow templates on Microsoft Azure for Red Hat Enterprise Linux
- DLab billing report functionality is not available on Microsoft Azure yet
- Microsoft Azure instance name length should not exceed 80 chars
- Low priority Virtual Machines are not supported yet on Microsoft Azure
