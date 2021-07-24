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

plugins {
    id(Plugins.Ktlint.id) version Plugins.Ktlint.version
}

tasks.create<Delete>(name = "clean") {
    delete = setOf(rootProject.buildDir)
}

subprojects {
    apply(plugin = Plugins.Ktlint.id)

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        debug.set(true)
    }
}
