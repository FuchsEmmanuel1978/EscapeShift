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
        versionCode = 33
        versionName = "2.9.0-v31-world3-24-30"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    sourceSets {
        getByName("main") {
            res.srcDirs(
                "src/main/res",
                "src/main/graphics/worlds",
                "src/main/graphics/hero",
                "src/main/graphics/global",
                "src/main/graphics/objects",
                "src/main/graphics/enemies",
                "src/main/graphics/animations"
            )
        }
    }
}


dependencies {
    implementation("com.startapp:inapp-sdk:5.2.4")
    testImplementation("junit:junit:4.13.2")
    implementation("com.google.android.gms:play-services-ads:24.0.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    implementation("com.android.billingclient:billing:8.2.1")
}
