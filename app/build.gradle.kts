plugins {
    id("com.android.application")
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.mycampusmap"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mycampusmap"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat.v170)
    implementation(libs.play.services.location) // Location services
    implementation(libs.lifecycle.runtime.ktx.v260) // Replacing lifecycle extensions
    implementation(libs.material.v190)
    implementation(libs.constraintlayout)
    implementation(libs.multidex)
    implementation(libs.activity.ktx)
    implementation(libs.core)
    implementation(platform(libs.firebase.bom.v3390))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.core)
    implementation(libs.firebase.auth)



    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.v121)
    androidTestImplementation(libs.espresso.core.v361)
}







