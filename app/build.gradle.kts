plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.a3dshop"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.a3dshop"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // CameraX core library using camera2 implementation
    implementation("androidx.camera:camera-camera2:1.1.0-alpha05")
    // CameraX Lifecycle library
    implementation("androidx.camera:camera-lifecycle:1.1.0-alpha05")
    // CameraX View class
    implementation("androidx.camera:camera-view:1.0.0-alpha28")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}