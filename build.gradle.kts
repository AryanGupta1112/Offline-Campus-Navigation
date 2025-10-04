// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {

    // Add the dependency for the Google services Gradle plugin
    id("com.google.gms.google-services") version "4.3.13" apply false

}
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.gradle.v814) // Ensure this is 8.1 or higher
        classpath(libs.google.services) // Use the latest version
        classpath(libs.firebase.crashlytics.gradle) // Add this for Crashlytics
        classpath(libs.google.services.v4310)  // Make sure this line is present


    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory) // Updated to use layout.buildDirectory
}




