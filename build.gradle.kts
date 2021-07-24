// Top-level build file where you can add configuration options common
// to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath(Plugins.Android.gradle)
        classpath(Plugins.Kotlin.gradle)
    }
}

tasks.create<Delete>(name = "clean") {
    delete = setOf(rootProject.buildDir)
}
