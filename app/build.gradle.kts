plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.chaquopy)
}

base.archivesName = "hermes-voice"

android {
    namespace = "com.xnu.rocky"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xnu.rocky"
        minSdk = 28
        targetSdk = 35
        versionCode = (findProperty("VERSION_CODE") as? String)?.toIntOrNull() ?: 1
        versionName = (findProperty("VERSION_NAME") as? String) ?: "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    flavorDimensions += "pyVersion"
    productFlavors {
        create("standard") {
            dimension = "pyVersion"
        }
    }

    signingConfigs {
        create("release") {
            val ksFile = file("../keystore/openrocky-release.jks")
            if (ksFile.exists()) {
                storeFile = ksFile
                storePassword = findProperty("KEYSTORE_PASSWORD") as? String ?: System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = findProperty("KEY_ALIAS") as? String ?: System.getenv("KEY_ALIAS") ?: "openrocky"
                keyPassword = findProperty("KEY_PASSWORD") as? String ?: System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigningConfig = signingConfigs.findByName("release")
            if (releaseSigningConfig?.storeFile?.exists() == true) {
                signingConfig = releaseSigningConfig
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

chaquopy {
    defaultConfig {
        version = "3.11"
        pip {
            // Common useful packages
            install("requests")
            install("beautifulsoup4")
        }
    }
    productFlavors {
        getByName("standard") {
            version = "3.11"
        }
    }
    sourceSets {
        getByName("main") {
            srcDir("src/main/python")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.browser)
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation(libs.markdown.renderer)
    implementation(libs.markdown.renderer.coil3)
    implementation(libs.vico.compose.m3)
    implementation(libs.coil.compose)
    implementation("io.getstream:stream-webrtc-android:1.3.10")
    implementation("androidx.health.connect:connect-client:1.1.0-alpha10")

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.android)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
