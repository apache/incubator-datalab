# DLab Changelog

## Version 1.1 - Dec 29 2016

### New features in v.1.1
- Introduced the Changelog
- Added support of RStudio
- Added Jupyter R kernels support
- Added ggplot2 and reshape2 libraries support for RStudio and R kernels
- Added multiple AWS regions support
- Implemented DLab as a service
- Implemented Jupyter as a service
- Added support of non-default VPC
- Added scripts for initial infrastructure termination via Jenkins
- Added filters to DLab "List of Resources" screen
- Added Help page on how to create a public key
- Added Help page on how to access Notebook server
- Added support of MS Edge bowser for DLab

### Enhancements/changes in v.1.1:
- Support of latest version of EMR 5.2.0 has been added. Note: support of older versions of EMR is deprecated
- Spark 2.0.2 support added for local kernels of R, Scala, Python. Note: support of older versions of Spark is deprecated
- EMR versions up to 5.2.0 are no longer supported
- Changed naming convention for EMR and notebook instances
- Changed logs storage to /var/opt/dlab/log/, added log rotation
- Added DescribeInstanceStatus action to SSN policy
- Changed configuration files naming conventions and location
- Changed Spark instalaltion script so now Spark is downloaded for each cluster
- Hadoop configuration files are now placed in /opt/EMRVERSION/CLUSTERNAME/conf/
- Spark is now downloaded from EMR master and bunch of parameters are moved from spark-defaults to emr.ini file
- Сhange format and location of ssn creation response json file
- A bunch of parameters are being parsed and added to spark-defaults
- Moved to json as a STDIN for Docker
- Changed up_time stored in MongoDB to ISODate format
- Added limitations for using spaces in resources names
- Added exploratory_name check to avoid EMR creation failures
- Added --no-cache-dir for pip commands to avoid instances provisioning issues

### Fixes in v.1.1:
- Fixed SSN IAM role rollback
- Fixed issues with security group creation during edge node provisioning
- Fixed a problem with remote kermels not being cleared out in Jupyter if linked EMR was stopped
- Fixed EMR rollback in case when jars/YARN files have note been downloaded
- Fixed terminate notebook error handling in case when bucket does not exist
- Fixed minor issues with PY4J in local Spark kernels
- Fixed condition to determine what policy should be attached to edge/notebook roles
- Fixed EC2 instance creation failure (SSN, Edge) when IAM instance profile has not been created before
- Fixed bug when single SG id being passed to EDGE node
- Fixed date parse warnings handling
- Fixed health-checks returning incorrect statuses
- Fixed incorrect resources statuses on notebook termination
- Fixed incorrect handling of errors in docker response
- Fixed UI overlapping in case when there are more than 2 clusters created
- Fixed data caching in notebook/EMR details popups
- Fixed a problem when public key filename was being displayed incorrectly
- Fixed 401 errors handling
- Fixed notebook stop confirmation dialog in case when there are no running EMRs
- Fixed issue when long EMR name was breakig the layout
- Minor corrections of Chrome UI layout
