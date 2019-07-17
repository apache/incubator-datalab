# *****************************************************************************
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# ******************************************************************************
//variable "nginx_http_port" {
//    default = "31080"
//    description = "Sets the nodePort that maps to the Ingress' port 80"
//}
//variable "nginx_https_port" {
//    default = "31443"
//    description = "Sets the nodePort that maps to the Ingress' port 443"
//}

//variable "mongo_root_pwd" {
//    default = "$tr0ng_r00T-passworI)"
//    description = "Password for MongoDB root user"
//}
//variable "mongo_db_username" {
//    default = "admin"
//    description = "Password for MongoDB root user"
//}
variable "mongo_db_pwd" {
    default = "$tr0ng_N0N=r00T-passworI)"
    description = "Password for MongoDB root user"
}
//variable "mongo_dbname" {
//    default = "dlabdb"
//    description = "Password for MongoDB root user"
//}
//variable "image_tag" {
//    default = "4.0.10-debian-9-r13"
//    description = "MongoDB Image tag"
//}