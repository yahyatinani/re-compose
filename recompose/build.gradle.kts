plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    compileSdk = 30
    buildToolsVersion = "31.0.0"

    defaultConfig {
        minSdk = 22
        targetSdk = 30
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = Libs.jvmTarget
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(Libs.Androidx.coreKtx)
    implementation(Libs.Androidx.appcompat)
    implementation(Libs.Androidx.viewModelCompose)
    implementation(Libs.ReactiveX.rxKotlin)
    implementation(Libs.Y.collections)

    testImplementation(Libs.Kotlin.kotlinReflect)
    testImplementation(Libs.Kotest.runner)
    testImplementation(Libs.Kotest.assertions)
    testImplementation(Libs.Kotest.property)
}

val ossrhUsername: String by project
val ossrhPassword: String by project
val signingKey: String? by project
val signingPassword: String? by project

val pubs: PublicationContainer =
    (extensions.getByName("publishing") as PublishingExtension).publications

//val androidSourcesJar by tasks.creating(Jar::class) {
//    archiveClassifier.set("sources")
//    from(android.sourceSets.getByName("main").java.srcDirs)
//}

val androidSourcesJar by tasks.register<Jar>("androidSourcesJar") {
    archiveClassifier.set("sources")
    from(android.sourceSets.getByName("main").java.srcDirs)
}
val androidJavadoc by tasks.register<Javadoc>("androidJavadoc") {
    source = android.sourceSets.getByName("main").java.getSourceFiles()
    classpath += project.files(android.bootClasspath.joinToString { File.pathSeparator })
}

val androidJavadocJar by tasks.register<Jar>("androidJavadocJar") {
    dependsOn(androidJavadoc)
    archiveClassifier.set("javadoc")
    from(androidJavadoc.destinationDir)
    isPreserveFileTimestamps =false
    isReproducibleFileOrder = true
}

apply(from = "../signing-pom-details.gradle.kts")

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components.getByName("release"))
            }
        }

        // Package the source code
        publications.withType<MavenPublication>().forEach {
            it.apply {
                artifact(androidSourcesJar)
                artifact(androidJavadocJar)
            }
        }

        // TODO: Package the Docs
    }
}
