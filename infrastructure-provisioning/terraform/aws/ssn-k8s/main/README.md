# Terraform module for deploying DataLab SSN K8S cluster

List of variables which should be provided:

| Variable                 | Type   | Description/Value                                                                                         |
|--------------------------|--------|-----------------------------------------------------------------------------------------------------------|
| access\_key\_id          | string | **Required.** AWS Access Key ID.                                                                          |
| secret\_access\_key      | string | **Required.** AWS Secret Access Key.                                                                      |
| service\_base\_name      | string | Any infrastructure value (should be unique if multiple SSN’s have been deployed before). Default: datalab-k8s|
| vpc\_id                  | string | ID of AWS VPC if you already have VPC created.                                                            | 
| vpc\_cidr                | string | CIDR for VPC creation. Conflicts with _vpc\_id_. Default: 172.31.0.0/16                                   |
| subnet\_id               | string | ID of AWS Subnet if you already have subnet created.                                                      |
| subnet\_cidr             | string | CIDR for Subnet creation. Conflicts with _subnet\_id_. Default: 172.31.0.0/24                             |
| env\_os                  | string | OS type. Available options: debian, redhat. Default: debian                                               |
| ami                      | string | **Required.** ID of EC2 AMI.                                                                              |
| key\_name                | string | **Required.** Name of EC2 Key pair.                                                                       |
| region                   | string | Name of AWS region. Default: us-west-2                                                                    |
| zone                     | string | Name of AWS zone. Default: a                                                                              |
| ssn\_k8s\_masters\_count | int    | Count of K8S masters. Default: 3                                                                          |
| ssn\_k8s\_workers\_count | int    | Count of K8S workers. Default: 2                                                                          |
| ssn\_root\_volume\_size  | int    | Size of root volume in GB. Default: 30                                                                    |
| allowed\_cidrs           | list   | CIDR to allow acces to SSN K8S cluster. Default: 0.0.0.0/0                                                |
| ssn\_k8s\_masters\_shape | string | Shape for SSN K8S masters. Default: t2.medium                                                             |
| ssn\_k8s\_workers\_shape | string | Shape for SSN K8S workers. Default: t2.medium                                                             |
| os\_user                 | string | Name of DataLab service user. Default: datalab-user                                                             |