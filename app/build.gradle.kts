plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.escapeshift.game"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.escapeshift.game"
        minSdk = 24
        targetSdk = 35
        versionCode = 14
        versionName = "1.4.0-audio"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}


dependencies {
    testImplementation("junit:junit:4.13.2")
}
