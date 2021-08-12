# Terraform module for deploying DataLab Endpoint instance

List of variables which should be provided:

| Variable                 | Type   | Description/Value                                                                                                                                   |
|--------------------------|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| service\_base\_name      | string | Any infrastructure value (should be unique if multiple SSN’s have been deployed before). Should be same as on ssn                                   |
| vpc\_id                  | string | ID of AWS VPC if you already have VPC created.                                                                                                      | 
| vpc\_cidr                | string | CIDR for VPC creation. Conflicts with _vpc\_id_. Default: 172.31.0.0/16                                                                             |
| subnet\_id               | string | ID of AWS Subnet if you already have subnet created.                                                                                                |
| subnet\_cidr             | string | CIDR for Subnet creation. Conflicts with _subnet\_id_. Default: 172.31.0.0/24                                                                       |
| ami                      | string | **Required** ID of EC2 AMI. Default ubuntu 18.04.1 (debian os): "ami-08692d171e3cf02d6" (aws ami: 258751437250/ami-ubuntu-18.04-1.13.0-00-1543963388|
| key\_name                | string | **Required** Name of EC2 Key pair. (Existed on AWS account)                                                                                         |
| region                   | string | Name of AWS region. Default: us-west-2                                                                                                              |
| zone                     | string | Name of AWS zone. Default: a                                                                                                                        |                    
| endpoint\_volume\_size   | int    | Size of root volume in GB. Default: 30                                                                                                              |
| network\_type            | string | Type of created network (if network is not existed and require creation) for endpoint. Default: public                                              |
| endpoint\_instance\_shape| string | Instance shape of Endpoint. Default: t2.medium                                                                                                      |