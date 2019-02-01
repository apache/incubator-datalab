import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.repository.storage.WritePolicy


ubuntuProxyConfiguration = new Configuration(
        repositoryName: "apt-ubuntu",
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
        repositoryName: "apt-security",
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
        repositoryName: "apt-bintray",
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
        repositoryName: "rrutter-proxy",
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
RProxyConfiguration = new Configuration(
        repositoryName: "r-repo",
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
                        remoteUrl: 'http://cran.us.r-project.org',
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