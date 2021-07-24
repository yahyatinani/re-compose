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
        jvmTarget = "1.8"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}


dependencies {
    implementation("androidx.core:core-ktx:1.6.0")
    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("com.google.android.material:material:1.4.0")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:1.0.0-alpha07")
    implementation("io.reactivex.rxjava3:rxkotlin:3.0.1")
    implementation("com.github.whyrising.y:y-collections:0.0.2.1")

    testImplementation("org.jetbrains.kotlin:kotlin-reflect:1.5.10")

    val version = "4.6.1"
    testImplementation("io.kotest:kotest-runner-junit5:$version")
    testImplementation("io.kotest:kotest-assertions-core:$version")
    testImplementation("io.kotest:kotest-property:$version")

    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}


tasks.withType<Test> {
    useJUnitPlatform()
}