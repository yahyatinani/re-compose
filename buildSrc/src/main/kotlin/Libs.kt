object Libs {
    const val jvmTarget = "1.8"

    object Kotlin {
        const val version = "1.5.30"

        const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:$version"
    }

    object Compose {
        private const val gr = "androidx.compose"
        const val version = "1.0.3"

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
        // Integration with activities
        const val activityCompose =
            "androidx.activity:activity-compose:1.3.1"

        // Integration with ViewModels
        const val viewModelCompose =
            "androidx.lifecycle:lifecycle-viewmodel-compose:1.0.0-alpha07"

        // Appcompat is needed for themes.xml resource
        const val appcompat = "androidx.appcompat:appcompat:1.3.1"

        const val navigationCompose =
            "androidx.navigation:navigation-compose:2.4.0-alpha10"

        const val constraintLayoutCompose =
            "androidx.constraintlayout:constraintlayout-compose:1.0.0-rc01"

        const val coreKtx = "androidx.core:core-ktx:1.6.0"
    }

    object Accompanist {
        private const val version = "0.19.0"
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

        const val vmLifecycle =
            "androidx.lifecycle:lifecycle-viewmodel-ktx:2.3.1"
    }

    object Y {
        private const val group = "com.github.whyrising.y"
        private const val version = "0.0.7"

        const val core = "$group:y-core:$version"
        const val collections = "$group:y-collections:$version"
        const val concurrency = "$group:y-concurrency:$version"
    }

    object Kotest {
        private const val version = "5.0.0.M2"

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
