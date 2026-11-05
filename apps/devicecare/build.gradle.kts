plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "online.capitalai.devicecare"
    compileSdk = 36
    defaultConfig {
        applicationId = "online.capitalai.devicecare"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-m1-preview"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:evidence"))
    implementation(project(":features:battery"))
    implementation(project(":features:permissions"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
