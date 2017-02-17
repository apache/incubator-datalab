# DLab is Self-service, Fail-safe Exploratory Environment for Collaborative Data Science Workflow
## New features in v1.2

•	Added support of another analytical tool - Zeppelin v.0.6.2
•	Introduced initial version of user preferences - dashboard filtering criteria is now stored into database per user
•	Added a possibility to Show Active/Show All instances with a single click of the mouse
•	Added support of AWS Signature Version 4q
•	Instance shapes are now grouped based on T-Shirt sizes (configurable)
•	User's S3 bucket name, Edge node IP address, type of notebook are now displayed on analytical tool details popup
•	Added "Page not found" component

## Improvements in v.1.2

•	On Notebook Start action - new IP address gets propagated to DLAB in cases when notebook has been stopped and its IP has changed in AWS
•	AWS instance creation is now rolled backed in AWS in cases when AMI file creation fails
•	Added a checksum comparison step for downloading all tar.gz files during analytical tool provisioning
•	Improved logging and error handling in self-service and provisioning-service
•	Improved error handling in boto scripts 
 
## Bug fixes in v.1.2
 
•	Fixed a problem when terminating EMR for R-studio commented out local SPARK_HOME in environment variables
•	Fixed a problem when 401 response was returned after logout
•	Fixed couple of security issues in back-end and Node JS codebases
•	Fixed a problem when termination of infrastructure was failing if service base name (SBN) was part of existing SBN
•	Fixed a rare problem when local kernels failed on "Unable to instantiate org.apache.hadoop.hive.ql.metadata.SessionHiveMetaStoreClient"
•	Fixed a problem when wrong environment variables were set in case if multiple EMR were attached to a notebook
