plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.escapeshift.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.escapeshift.game"
        minSdk = 24
        targetSdk = 36
        versionCode = 14
        versionName = "1.4.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildTypes {
        getByName("release") {
            // Create the signed AAB with Android Studio's signing wizard.
            isMinifyEnabled = false
            isDebuggable = false
        }
    }
}


dependencies {
    testImplementation("junit:junit:4.13.2")
}
