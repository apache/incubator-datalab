/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.repository.storage.WritePolicy


ubuntuProxyConfiguration = new Configuration(
        repositoryName: "ubuntu",
        recipeName: "apt-proxy",
        online: true,
        attributes: [
                storage: [
                        blobStoreName              : 'packages_store',
                        writePolicy                : WritePolicy.ALLOW,
                        strictContentTypeValidation: true
                ] as Map,
                apt: [
                        distribution : 'xenial',
                        flat : false
                ] as Map,
                httpclient   : [
                        connection: [
                                blocked  : false,
                                autoBlock: true
                        ] as Map
                ] as Map,
                proxy: [
                        remoteUrl: 'http://REGION.ec2.archive.ubuntu.com/ubuntu/',
                        contentMaxAge: 0,
                        metaDataMaxAge: 0
                ] as Map,
                negativeCache: [
                        enabled   : true,
                        timeToLive: 1440
                ] as Map,
        ] as Map
)
securityProxyConfiguration = new Configuration(
        repositoryName: "ubuntu-security",
        recipeName: "apt-proxy",
        online: true,
        attributes: [
                storage: [
                        blobStoreName              : 'packages_store',
                        writePolicy                : WritePolicy.ALLOW,
                        strictContentTypeValidation: true
                ] as Map,
                apt: [
                        distribution : 'xenial',
                        flat : false
                ] as Map,
                httpclient   : [
                        connection: [
                                blocked  : false,
                                autoBlock: true
                        ] as Map
                ] as Map,
                proxy: [
                        remoteUrl: 'http://security.ubuntu.com/ubuntu',
                        contentMaxAge: 0,
                        metaDataMaxAge: 0
                ] as Map,
                negativeCache: [
                        enabled   : true,
                        timeToLive: 1440
                ] as Map,
        ] as Map
)
BintrayDebianProxyConfiguration = new Configuration(
        repositoryName: "ubuntu-bintray",
        recipeName: "apt-proxy",
        online: true,
        attributes: [
                storage: [
                        blobStoreName              : 'packages_store',
                        writePolicy                : WritePolicy.ALLOW,
                        strictContentTypeValidation: true
                ] as Map,
                apt: [
                        distribution : 'xenial',
                        flat : false
                ] as Map,
                httpclient   : [
                        connection: [
                                blocked  : false,
                                autoBlock: true
                        ] as Map
                ] as Map,
                proxy: [
                        remoteUrl: 'https://dl.bintray.com/sbt/debian',
                        contentMaxAge: 0,
                        metaDataMaxAge: 0
                ] as Map,
                negativeCache: [
                        enabled   : true,
                        timeToLive: 1440
                ] as Map,
        ] as Map
)
RrutterDebianProxyConfiguration = new Configuration(
        repositoryName: "rrutter",
        recipeName: "apt-proxy",
        online: true,
        attributes: [
                storage: [
                        blobStoreName              : 'packages_store',
                        writePolicy                : WritePolicy.ALLOW,
                        strictContentTypeValidation: true
                ] as Map,
                apt: [
                        distribution : 'xenial',
                        flat : false
                ] as Map,
                httpclient   : [
                        connection: [
                                blocked  : false,
                                autoBlock: true
                        ] as Map
                ] as Map,
                proxy: [
                        remoteUrl: 'http://ppa.launchpad.net/marutter/rrutter/ubuntu',
                        contentMaxAge: 0,
                        metaDataMaxAge: 0
                ] as Map,
                negativeCache: [
                        enabled   : true,
                        timeToLive: 1440
                ] as Map,
        ] as Map
)
CanonicalDebianProxyConfiguration = new Configuration(
        repositoryName: "ubuntu-canonical",
        recipeName: "apt-proxy",
        online: true,
        attributes: [
                storage: [
                        blobStoreName              : 'packages_store',
                        writePolicy                : WritePolicy.ALLOW,
                        strictContentTypeValidation: true
                ] as Map,
                apt: [
                        distribution : 'xenial',
                        flat : false
                ] as Map,
                httpclient   : [
                        connection: [
                                blocked  : false,
                                autoBlock: true
                        ] as Map
                ] as Map,
                proxy: [
                        remoteUrl: 'http://archive.canonical.com/ubuntu',
                        contentMaxAge: 0,
                        metaDataMaxAge: 0
                ] as Map,
                negativeCache: [
                        enabled   : true,
                        timeToLive: 1440
                ] as Map,
        ] as Map
)
RProxyConfiguration = new Configuration(
        repositoryName: "r",
        recipeName: "r-proxy",
        online: true,
        attributes: [
                storage: [
                        blobStoreName              : 'packages_store',
                        writePolicy                : WritePolicy.ALLOW,
                        strictContentTypeValidation: true
                ] as Map,
                httpclient   : [
                        connection: [
                                blocked  : false,
                                autoBlock: true
                        ] as Map
                ] as Map,
                proxy: [
                        remoteUrl: 'https://cloud.r-project.org',
                        contentMaxAge: 0,
                        metaDataMaxAge: 0
                ] as Map,
                negativeCache: [
                        enabled   : true,
                        timeToLive: 1440
                ] as Map,
        ] as Map
)
repository.createRepository(RProxyConfiguration)
repository.createRepository(ubuntuProxyConfiguration)
repository.createRepository(securityProxyConfiguration)
repository.createRepository(BintrayDebianProxyConfiguration)
repository.createRepository(RrutterDebianProxyConfiguration)
repository.createRepository(CanonicalDebianProxyConfiguration)
