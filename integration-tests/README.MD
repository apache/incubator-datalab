# The automation tests

## What is it?
This is the module with automation tests for DataLab integration testing.

## How to run?

Automation tests could be ran from UNIX and Windows machines.
The prerequisites are:
* The environment is established
* The machine, where tests are going to be ran, has access to Jenkins
* Set of configurations are performed
* Set of Java parameters are passed.

## What are java parameters?

The Java parameters are key value pairs prefixed with "-D" passed to Java 
Required Java parameters are: "jenkins.buildNumber", "cluster.username", "cluster.os.family"

Example:
* -Djenkins.buildNumber=57 -Dcluster.username=datalab-user -Dcluster.os.family=debian

## What are other required configurations?

### The config.properties file
 
 "config.properties" file example could be found in the module root "example" directory.
 
### Jupyther scenario files
 Scenario files example can be found in module root "scenario_jupyter" directory.
 
### Amazon instances configuration json files
Amazon instances configuration json files examples can be found in module root "ec2_templates" directory.

## Automation run modes
 There are 3 modes: server and dev and dev local. Base on these modes the configuration files should be placed in different locations.
 
### Server mode 
Server does not require any specific parameters. The configuration files locations is following for server mode:
* conf.file.location=${conf.root.path}/config.properties
* keys.directory.location=${conf.root.path}/keys
* python.files.location=${conf.root.path}/scenario_jupyter/
* cluster.config.file.location=${conf.root.path}/ec2_templates/

### Dev mode.
Dev mode has 2 choices run against service deployed in Amazon or against local mocked services. 

#### Dev mode against services deployed locally
To be able to run tests against locally deployed service, it is required to pass following parameters:

 * -Dconf.root.path=examples

Also you should describe following parameters into config.properties file or to pass it to JVM:
 * -Drun.mode.local=true
 * -Duse.jenkins=false
 * -Dssn.url=https://localhost:8443
 * -Dservice.base.name=dev
 * -Dnotebooks.to.test=rstudio,jupyter,zeppelin

#### Dev mode against services deployed in Amazon
To be able to run tests against services deployed in Amazon, it is required to pass following parameters: 
 
 * -Drun.mode=dev
 * -Dconf.root.path=examples
 * -Djenkins.buildNumber=163
 * -Dnotebooks.to.test=rstudio
 * -Dexecution.threads=1


The "execution.threads" property with value 1  limits threads for notebooks testing to 1. So all tests would be executed synchronously.

To run automation tests in dev mode it is required to pass a few more Java parameters: 
* "run.mode" set to "dev".
* 


Example:
* -Drun.mode=dev.

In this case the application configuration is following:
