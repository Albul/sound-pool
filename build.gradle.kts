buildscript {
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath(Libs.plugin.kotlin_gradle)
        classpath(Libs.plugin.android_gradle)
        classpath(Libs.plugin.bintray_gradle)
        classpath(Libs.plugin.github_dcendents_gradle)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }
}