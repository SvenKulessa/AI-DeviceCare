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
        val requestedCode = providers.gradleProperty("versionCode").orElse("1").get()
        versionCode = requestedCode.toIntOrNull()?.takeIf { it in 1..2_100_000_000 }
            ?: error("versionCode must be an integer between 1 and 2100000000")
        versionName = providers.gradleProperty("versionName").orElse("0.1.0-m1-preview").get()
        require(versionName!!.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+(?:[-+][A-Za-z0-9.-]+)?"))) {
            "versionName must use major.minor.patch with an optional suffix"
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
}
