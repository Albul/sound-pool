plugins {
    id("com.android.library")
    kotlin("android")
}

val libraryVersion: String by project

android {
    compileSdkVersion(Versions.sdk.compile)
    buildToolsVersion(Versions.buildTools)

    defaultConfig {
        minSdkVersion(Versions.sdk.min)
        targetSdkVersion(Versions.sdk.target)
        versionCode = 1
        versionName = libraryVersion
    }
    compileOptions {
        encoding = "UTF-8"
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    java {
        sourceSets.getByName("main").java.srcDir("src/main/kotlin")
    }
    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/LICENSE")
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/license.txt")
        exclude("META-INF/NOTICE")
        exclude("META-INF/NOTICE.txt")
        exclude("META-INF/notice.txt")
        exclude("META-INF/ASL2.0")
        exclude("META-INF/*.kotlin_module")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Javadoc>().all {
    enabled = false
}

dependencies {
    implementation(Libs.olekdia.common_jvm)
    implementation(Libs.olekdia.common_android)
    implementation(Libs.olekdia.sparse_array_jvm)
    implementation(Libs.androidx.annotations)
}

apply(from = "bintray.gradle")