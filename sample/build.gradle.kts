import com.github.whyrising.recompose.Deps

plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdk = Deps.AndroidBuild.compileSdk
    buildToolsVersion = Deps.AndroidBuild.buildToolsVersion

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
        jvmTarget = Deps.jvmTarget
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Deps.Compose.version
    }

    packagingOptions {
        resources.excludes.add("META-INF/*")
    }
}

dependencies {
    implementation(Deps.Androidx.core)
    implementation(Deps.Androidx.appcompat)
    implementation(Deps.Compose.ui)
    implementation(Deps.Compose.material)
    implementation(Deps.Compose.uiTooling)
    implementation(Deps.Compose.uiToolingPreview)
    implementation(Deps.Androidx.activityCompose)
    implementation(Deps.Accompanist.systemuicontroller)
    implementation(Deps.Y.core)
    implementation(Deps.Coroutines.coroutinesTest)
    implementation("com.google.android.material:material:1.5.0")
    implementation(project(mapOf("path" to ":recompose")))

    testImplementation(Deps.Kotlin.kotlinReflect)
    testImplementation(Deps.Kotest.runner)
    testImplementation(Deps.Kotest.assertions)
    testImplementation(Deps.Kotest.property)
    testImplementation(Deps.Mockk.core)
    testImplementation(Deps.Mockk.jvm)

    androidTestImplementation(Deps.Compose.uiTestJUnit)
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
