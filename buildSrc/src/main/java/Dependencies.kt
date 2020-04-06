object Versions {
    const val kotlin = "1.3.71"
    const val mockk = "1.9.3"
    const val junit = "4.13"
    const val robolectric = "4.3.1"
    const val espresso = "3.2.0"

    object olekdia {
        const val common = "0.1.7"
        const val common_android = "3.2.9"
        const val common_android_test = "3.0.0"
    }

    object sdk {
        const val min = 16
        const val target = 29
        const val compile = 29
    }
    const val buildTools = "29.0.2"

    object androidx {
        const val annotations = "1.1.0"
        const val collections = "1.1.0"
        const val core = "1.2.0"
        const val core_ktx = "1.2.0"
        const val appcompat = "1.1.0"
        const val material = "1.1.0"
        const val fragment = "1.2.2"

        const val test_core = "1.2.0"
        const val test_runner = "1.2.0"
        const val test_rules = "1.2.0"
    }

    const val android_gradle = "3.5.3"
    const val bintray_gradle = "1.8.4"
    const val github_dcendents_gradle = "2.1"
}


object Libs {
    object kotlin {
        val reflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}"
    }
    val mockk_common = "io.mockk:mockk-common:${Versions.mockk}"
    val mockk_jvm = "io.mockk:mockk:${Versions.mockk}"
    val junit = "junit:junit:${Versions.junit}"
    val robolectric = "org.robolectric:robolectric:${Versions.robolectric}"
    val espresso = "androidx.test.espresso:espresso-core:${Versions.espresso}"

    object olekdia {
        private val common_prefix = "com.olekdia:multiplatform-common"
        val common = "$common_prefix:${Versions.olekdia.common}"
        val common_jvm = "$common_prefix-jvm:${Versions.olekdia.common}"
        val common_js = "$common_prefix-js:${Versions.olekdia.common}"
        val common_native = "$common_prefix-native:${Versions.olekdia.common}"

        val common_android = "com.olekdia:android-common:${Versions.olekdia.common_android}"
        val common_android_test = "com.olekdia:android-common-test:${Versions.olekdia.common_android_test}"
    }

    object androidx {
        val annotations = "androidx.annotation:annotation:${Versions.androidx.annotations}"
        val collections = "androidx.collection:collection:${Versions.androidx.collections}"
        val core = "androidx.core:core:${Versions.androidx.core}"
        val core_ktx = "androidx.core:core-ktx:${Versions.androidx.core_ktx}"
        val fragment = "androidx.fragment:fragment:${Versions.androidx.fragment}"
        val appcompat = "androidx.appcompat:appcompat:${Versions.androidx.appcompat}"
        val material = "com.google.android.material:material:${Versions.androidx.material}"

        val test_core = "androidx.test:core:${Versions.androidx.test_core}"
        val test_runner = "androidx.test:runner:${Versions.androidx.test_runner}"
        val test_rules = "androidx.test:rules:${Versions.androidx.test_rules}"
    }

    object plugin {
        val kotlin_gradle = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
        val android_gradle = "com.android.tools.build:gradle:${Versions.android_gradle}"
        val bintray_gradle = "com.jfrog.bintray.gradle:gradle-bintray-plugin:${Versions.bintray_gradle}"
        val github_dcendents_gradle = "com.github.dcendents:android-maven-gradle-plugin:${Versions.github_dcendents_gradle}"
    }
}
