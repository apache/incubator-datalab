# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

sc <- sparkR.session(MASTER)

working_storage <- "WORKING_STORAGE"
output_directory <- "rstudio"
protocol_name <- "PROTOCOL_NAME"

bucket_path <- function(file_path) {
    sprintf("%s://%s/rstudio_dataset/%s", protocol_name, working_storage, file_path)
}

full_path <- function(file_path) {
    sprintf("%s://%s/%s/%s", protocol_name, working_storage, output_directory, file_path)
}

carriers <- read.df(bucket_path("carriers.csv"), "csv", header="true", inferSchema="true")
write.df(carriers, path=full_path("carriers"), source="parquet", mode="overwrite")
createOrReplaceTempView(carriers, "carriers")

airports <- read.df(bucket_path("airports.csv"), "csv", header="true", inferSchema="true")
write.df(airports, path=full_path("airports"), source="parquet", mode="overwrite")
createOrReplaceTempView(airports, "airports")

flights_w_na <- read.df(bucket_path("2008.csv.bz2"), "csv", header="true", inferSchema="true")
flights <- fillna(flights_w_na, 0, cols=colnames(flights_w_na)[c(15, 16, 25:29)])
write.df(flights, path=full_path("flights"), source="parquet", mode="overwrite")
createOrReplaceTempView(flights, "flights")
colnames(flights)
