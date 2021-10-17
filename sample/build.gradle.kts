plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdk = 31
    buildToolsVersion = "31.0.0"

    defaultConfig {
        applicationId = "com.github.whyrising.recompose"
        minSdk = 22
        targetSdk = 30
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Libs.Compose.version
    }

    packagingOptions {
        resources.excludes.add("META-INF/*")
    }
}

dependencies {
    implementation(Libs.Androidx.coreKtx)
    implementation(Libs.Androidx.appcompat)
    implementation(Libs.Compose.ui)
    implementation(Libs.Compose.material)
    implementation(Libs.Compose.uiTooling)
    implementation(Libs.Compose.uiToolingPreview)
    implementation(Libs.Androidx.activityCompose)
    implementation(Libs.Accompanist.systemuicontroller)
    implementation(Libs.Y.collections)
    implementation(Libs.Y.concurrency)
    implementation(Libs.Coroutines.coroutinesTest)
    implementation("com.google.android.material:material:1.4.0")
    implementation(project(mapOf("path" to ":recompose")))

    testImplementation(Libs.Kotlin.kotlinReflect)
    testImplementation(Libs.Kotest.runner)
    testImplementation(Libs.Kotest.assertions)
    testImplementation(Libs.Kotest.property)
    testImplementation(Libs.Mockk.core)
    testImplementation(Libs.Mockk.jvm)

    androidTestImplementation(Libs.Compose.uiTestJUnit)
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
