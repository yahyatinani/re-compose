package com.github.whyrising.recompose

object Libs {
    const val jvmTarget = "1.8"
    const val jdkDesugar = "com.android.tools:desugar_jdk_libs:1.1.5"

    object AndroidBuild {
        const val compileSdk = 32
        const val buildToolsVersion = "32.1.0-rc1"
    }

    object Kotlin {
        const val version = "1.5.31"
        const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:$version"
    }

    object Compose {
        private const val gr = "androidx.compose"
        const val version = "1.0.4"

        const val ui = "$gr.ui:ui:$version"

        // Tooling support (Previews, etc.)
        const val uiTooling = "$gr.ui:ui-tooling:$version"

        const val uiToolingPreview = "$gr.ui:ui-tooling-preview:$version"

        // Foundation (Border, Background, Box, Image, shapes, animations, etc.)
        const val foundation = "$gr.foundation:foundation:$version"

        // Material design
        const val material = "$gr.material:material:$version"

        // Material design icons
        const val iconsCore = "$gr.material:material-icons-core:$version"
        const val iconsExt = "$gr.material:material-icons-extended:$version"

        const val runtime = "androidx.compose.runtime:runtime:$version"

        // UI Testing
        const val uiTestJUnit = "$gr.ui:ui-test-junit4:$version"
    }

    object Androidx {
        private const val gr = "androidx"

        // Kotlin
        const val core = "$gr.core:core-ktx:1.7.0-rc01"

        // Integration with activities
        const val activityCompose = "$gr.activity:activity-compose:1.4.0"

        // Appcompat is needed for themes.xml resource
        const val appcompat = "$gr.appcompat:appcompat:1.4.0-beta01"
    }

    object Lifecycle {
        private const val group = "androidx.lifecycle"
        private const val version = "2.4.0-rc01"

        // ViewModel
        const val viewModel = "$group:lifecycle-viewmodel-ktx:$version"

        // ViewModel utilities for Compose
        const val VmCompose = "$group:lifecycle-viewmodel-compose:$version"
    }

    object Accompanist {
        private const val version = "0.20.0"
        private const val group = "com.google.accompanist"

        const val systemuicontroller =
            "$group:accompanist-systemuicontroller:$version"
    }

    object Coroutines {
        private const val group = "org.jetbrains.kotlinx"
        private const val version = "1.5.2"

        const val core = "$group:kotlinx-coroutines-core:$version"
        const val android = "$group:kotlinx-coroutines-android:$version"
        const val coroutinesTest = "$group:kotlinx-coroutines-test:$version"
    }

    object Y {
        private const val group = "com.github.whyrising.y"
        private const val version = "0.0.10"

        const val core = "$group:y-core:$version"
        const val collections = "$group:y-collections:$version"
        const val concurrency = "$group:y-concurrency:$version"
    }

    object Kotest {
        private const val version = "5.1.0"

        const val runner = "io.kotest:kotest-runner-junit5:$version"
        const val assertions = "io.kotest:kotest-assertions-core:$version"
        const val property = "io.kotest:kotest-property:$version"
    }

    object Mockk {
        private const val version = "1.12.0"

        const val core = "io.mockk:mockk:$version"
        const val jvm = "io.mockk:mockk-agent-jvm:$version"
    }
}
