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
    id("maven-publish")
    signing
}

allprojects {
    group = "com.github.whyrising.recompose"
    version = Ci.publishVersion
}

subprojects {
    apply(plugin = Plugins.Ktlint.id)

    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        debug.set(true)
    }

    tasks.withType<Test> {
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2)
            .takeIf { it > 0 } ?: 1

        useJUnitPlatform()
        testLogging { events("passed", "skipped", "failed") }
    }
}

val publications: PublicationContainer =
    (extensions.getByName("publishing") as PublishingExtension).publications

signing {
    useGpgCmd()
    if (Ci.isRelease())
        sign(publications)
}
