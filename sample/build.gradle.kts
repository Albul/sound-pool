plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdk = Versions.sdk.compile
    buildToolsVersion = Versions.buildTools

    defaultConfig {
        minSdk = Versions.sdk.min
        targetSdk = Versions.sdk.target
        applicationId = "com.olekdia.sample"
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    testOptions.unitTests.isIncludeAndroidResources = true

    compileOptions {
        encoding = "UTF-8"
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
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

dependencies {
    implementation(kotlin("stdlib", Versions.kotlin))
    implementation(Libs.olekdia.common_jvm)
    implementation(Libs.olekdia.common_android)
    implementation(project(":sound-pool"))

    implementation(Libs.androidx.annotations)
    implementation(Libs.androidx.fragment)
    implementation(Libs.androidx.appcompat)
    implementation(Libs.androidx.material)

    androidTestImplementation(Libs.junit)
    androidTestImplementation(Libs.espresso)
    androidTestImplementation(Libs.androidx.test_core)
    androidTestImplementation(Libs.androidx.test_runner)
    androidTestImplementation(Libs.androidx.test_rules)
}