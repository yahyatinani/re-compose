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

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Libs.Compose.version
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
    implementation(Libs.Compose.runtime)

    implementation(Libs.Coroutines.core)
    implementation(Libs.Coroutines.android)
    implementation(Libs.Coroutines.vmLifecycle)

    implementation(Libs.Y.core)
    implementation(Libs.Y.collections)
    implementation(Libs.Y.concurrency)

    testImplementation(Libs.Kotlin.kotlinReflect)
    testImplementation(Libs.Kotest.runner)
    testImplementation(Libs.Kotest.assertions)
    testImplementation(Libs.Kotest.property)
    testImplementation(Libs.Mockk.core)
    testImplementation(Libs.Mockk.jvm)
}

val androidSourcesJar by tasks.register<Jar>("androidSourcesJar") {
    archiveClassifier.set("sources")
    from(android.sourceSets.getByName("main").java.srcDirs)
}

val androidJavadoc by tasks.register<Javadoc>("androidJavadoc") {
    source = android.sourceSets.getByName("main").java.getSourceFiles()
    classpath += project.files(
        android.bootClasspath.joinToString { File.pathSeparator }
    )
}

val androidJavadocJar by tasks.register<Jar>("androidJavadocJar") {
    dependsOn(androidJavadoc)
    archiveClassifier.set("javadoc")
    from(androidJavadoc.destinationDir)
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

afterEvaluate {
    apply(from = "../signing-pom-details.gradle.kts")

    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components.getByName("release"))
            }
        }

        publications.withType<MavenPublication>().forEach {
            it.apply {
                artifact(androidSourcesJar)
                artifact(androidJavadocJar)

                pom {
                    val devUrl = "https://github.com/whyrising"
                    val libUrl = "$devUrl/re-compose"

                    name.set("Re-compose")
                    description.set("Event Driven Android UI Framework")
                    url.set(libUrl)

                    scm {
                        connection.set("scm:git:$libUrl")
                        developerConnection.set("scm:git:$devUrl")
                        url.set(libUrl)
                    }

                    licenses {
                        license {
                            name.set("MIT")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }

                    developers {
                        developer {
                            id.set("whyrising")
                            name.set("Yahya Tinani")
                            email.set("yahyatinani@gmail.com")
                        }
                    }
                }
            }
        }
    }
}
