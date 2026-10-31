plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "online.capitalai.devicecare.plus"
    compileSdk = 36
    defaultConfig {
        applicationId = "online.capitalai.devicecare.plus"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-p0"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation(project(":core:ui"))
    implementation("androidx.activity:activity-compose:1.13.0")
}
