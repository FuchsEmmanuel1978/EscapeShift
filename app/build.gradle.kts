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
        versionCode = 31
        versionName = "2.9.0-v27-world3-24-30"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}


dependencies {
    implementation("com.startapp:inapp-sdk:5.2.4")
    testImplementation("junit:junit:4.13.2")
}
