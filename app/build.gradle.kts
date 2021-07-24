plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdk = 30
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
}

dependencies {
    implementation(Libs.Androidx.coreKtx)
    implementation(Libs.Androidx.appcompat)
    implementation(Libs.Compose.ui)
    implementation(Libs.Compose.material)
    implementation(Libs.Compose.uiTooling)
    implementation(Libs.Compose.uiToolingPreview)
    implementation("com.google.android.material:material:1.4.0")
    implementation(Libs.Androidx.activityCompose)

    implementation(project(mapOf("path" to ":recompose")))

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation(Libs.Compose.uiTestJUnit)
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
