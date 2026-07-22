plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.linguatycoon.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.linguatycoon.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "DEFAULT_API_URL", "\"http://10.0.2.2:8000\"")
        buildConfigField("String", "GITHUB_REPOSITORY", "\"player1error/lingua-tycoon\"")
    }

    buildFeatures { buildConfig = true }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
