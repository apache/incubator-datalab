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

repository.createRepository(ubuntuProxyConfiguration)