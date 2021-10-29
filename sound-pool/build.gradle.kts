import java.util.*
import org.gradle.api.publish.PublishingExtension

plugins {
    id("com.android.library")
    kotlin("android")
    id("io.codearte.nexus-staging") version "0.30.0"
    `maven-publish`
    signing
}

val libraryVersion: String by project

android {
    compileSdk = Versions.sdk.compile
    buildToolsVersion = Versions.buildTools

    defaultConfig {
        minSdk = Versions.sdk.min
        targetSdk = Versions.sdk.target
    }
    compileOptions {
        encoding = "UTF-8"
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlin {
        sourceSets["main"].apply {
            kotlin.srcDir("src/main/kotlin")
        }
    }
    packagingOptions {
        resources.excludes.add("META-INF/DEPENDENCIES")
        resources.excludes.add("META-INF/LICENSE")
        resources.excludes.add("META-INF/LICENSE.txt")
        resources.excludes.add("META-INF/license.txt")
        resources.excludes.add("META-INF/NOTICE")
        resources.excludes.add("META-INF/NOTICE.txt")
        resources.excludes.add("META-INF/notice.txt")
        resources.excludes.add("META-INF/ASL2.0")
        resources.excludes.add("META-INF/*.kotlin_module")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Javadoc>().all {
    enabled = false
}

tasks {
    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(android.sourceSets.getByName("main").java.srcDirs)
    }

    artifacts {
        archives(sourcesJar)
    }
}

dependencies {
    implementation(Libs.olekdia.common_jvm)
    implementation(Libs.olekdia.common_android)
    implementation(Libs.olekdia.sparse_array_jvm)
    implementation(Libs.androidx.annotations)
}


//--------------------------------------------------------------------------------------------------
//  Publishing
//--------------------------------------------------------------------------------------------------

val fis = project.rootProject.file("local.properties").inputStream()
val properties = Properties().apply {
    load(fis)
}
val ossUser = properties.getProperty("oss.user")
val ossPassword = properties.getProperty("oss.password")
extra["signing.keyId"] = properties.getProperty("signing.keyId")
extra["signing.password"] = properties.getProperty("signing.password")
extra["signing.secretKeyRingFile"] = properties.getProperty("signing.secretKeyRingFile")

val publishedGroupId: String by project
val artifactName: String by project
val bintrayRepo: String by project
val libraryName: String by project
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
        maven("https://oss.sonatype.org/service/local/staging/deploy/maven2/") {
            name = "sonatype"
            credentials {
                username = ossUser
                password = ossPassword
            }
        }
    }
}

signing {
    sign(publishing.publications)
}

nexusStaging {
    username = ossUser
    password = ossPassword
    packageGroup = publishedGroupId
}