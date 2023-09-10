import io.github.yahyatinani.recompose.Ci.groupId
import io.github.yahyatinani.recompose.Versions

plugins {
  id("kotlin-conventions")
  `android-library`
  id("recompose.publishing-conventions")
}

group = groupId

android {
  namespace = groupId
  compileSdk = 34

  buildFeatures {
    compose = true
    buildConfig = false
  }

  defaultConfig {
    minSdk = 22
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
    aarMetadata {
      minCompileSdk = 22
    }
  }

  packaging {
    resources {
      excludes += listOf("/*.jar", "/META-INF/{AL2.0,LGPL2.1,INDEX.LIST}")
    }
  }

  buildTypes {
    release {
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
    kotlinCompilerExtensionVersion = Versions.COMPOSE_COMPILER
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
  }
}
