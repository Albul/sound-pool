import java.io.FileInputStream
import java.util.*
import org.gradle.api.publish.PublishingExtension

apply(plugin = "maven-publish")

val fis = FileInputStream("local.properties")
val properties = Properties().apply {
    load(fis)
}
val bintrayUser = properties.getProperty("bintray.user")
val bintrayApiKey = properties.getProperty("bintray.apikey")
val bintrayPassword = properties.getProperty("bintray.gpg.password")
val libraryVersion: String by project
val publishedGroupId: String by project
val artifactName: String by project
val bintrayRepo: String by project
val libraryName: String by project
val bintrayName: String by project
val libraryDescription: String by project
val siteUrl: String by project
val gitUrl: String by project
val licenseName: String by project
val licenseUrl: String by project
val developerOrg: String by project
val developerName: String by project
val developerEmail: String by project
val developerId: String by project

project.group = publishedGroupId
project.version = libraryVersion


configure<PublishingExtension> {
    publications {
        create<MavenPublication>("maven") {
            groupId = publishedGroupId
            artifactId = artifactName
            version = libraryVersion

            artifact(tasks.findByName("sourcesJar"))
            artifact("$buildDir/outputs/aar/${project.name}-release.aar")

            pom {
                name.set(libraryName)
                description.set(libraryDescription)
                url.set(siteUrl)

                licenses {
                    license {
                        name.set(licenseName)
                        url.set(licenseUrl)
                    }
                }
                developers {
                    developer {
                        id.set(developerId)
                        name.set(developerName)
                        email.set(developerEmail)
                    }
                }
                organization {
                    name.set(developerOrg)
                }
                scm {
                    connection.set(gitUrl)
                    developerConnection.set(gitUrl)
                    url.set(siteUrl)
                }
                withXml {
                    val dependenciesNode = asNode().appendNode("dependencies")

                    // Iterate over the implementation dependencies (we don't want the test ones), adding a <dependency> node for each
                    configurations.named("implementation").get().allDependencies.forEach {
                        // Ensure dependencies such as fileTree are not included in the pom.
                        if (it.name != "unspecified") {
                            val dependencyNode = dependenciesNode.appendNode("dependency")
                            dependencyNode.appendNode("groupId", it.group)
                            dependencyNode.appendNode("artifactId", it.name)
                            dependencyNode.appendNode("version", it.version)
                            project.logger.info("Added dependency: '${it.group}:${it.name}:${it.version}'")
                        }
                    }
                }
            }
        }
    }

    repositories {
        maven("https://api.bintray.com/maven/${developerOrg}/${bintrayRepo}/${artifactName}/;publish=1") {
            credentials {
                username = bintrayUser
                password = bintrayApiKey
            }
        }
    }
}