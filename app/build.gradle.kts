import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Which PWF Auth app this demo signs in to: local.properties (PWF_BASE_URL, PWF_APP_SECRET) or env vars of the same name.
val local = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun setting(name: String, fallback: String = "") =
    (local.getProperty(name)?.trim()?.ifEmpty { null } ?: System.getenv(name)?.trim()?.ifEmpty { null } ?: fallback).trim()
val pwfBase = setting("PWF_BASE_URL", "https://pwfauth.com").trimEnd('/')
val pwfSecret = setting("PWF_APP_SECRET")
if (pwfSecret.isEmpty()) logger.warn("PWF_APP_SECRET is not set in local.properties: every sign-in will be refused.")

android {
    namespace = "com.pwfauth.demo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pwfauth.demo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "PWF_BASE_URL", "\"$pwfBase\"")
        buildConfigField("String", "PWF_APP_SECRET", "\"$pwfSecret\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signed with the debug key so the APK installs straight away. Use your own key for Google Play.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
