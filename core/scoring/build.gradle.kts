plugins { id("com.android.library") }
android {
    namespace = "online.capitalai.devicecare.core.scoring"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation(project(":core:evidence"))
    testImplementation("junit:junit:4.13.2")
}
