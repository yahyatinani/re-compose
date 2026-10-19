# re-compose

[![Build](https://github.com/yahyatinani/re-compose/actions/workflows/main.yml/badge.svg)](https://github.com/yahyatinani/re-compose/actions/workflows/main.yml)
[![Snapshots](https://img.shields.io/badge/snapshots-available-orange)](https://central.sonatype.com/repository/maven-snapshots/io/github/yahyatinani/recompose/re-compose/maven-metadata.xml)
[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](https://opensource.org/licenses/GPL-3.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

> Event-driven, single-atom state management for Jetpack Compose — backed by persistent data structures.

**Status:** `experimental`

## Why?

Compose's built-in state works well for leaf-level UI. It gets harder when you need:

- A **single, auditable source of truth** across screens and features
- **Deterministic transitions** from event → next state, instead of `mutableStateOf` scattered through composables
- **Time-travel debugging** — the ability to replay every event and see the state it produced
- **Business logic that survives configuration changes** without being tied to composable lifecycles
- **Finite state machines** for UI with real modes (loading, error, empty, content, retry)

## Features

- 🧠 **Single-atom state** — the app domain lives in one immutable `IPersistentMap`, updated only by event handlers
- 📮 **Event dispatch** — intents are serialized as event vectors (`v(":event-name", args...)`) through a central router
- 🔍 **Reactive subscriptions** — 1st- and 2nd-degree subscriptions map state slices onto `StateFlow<Derived>`
- 🎛️ **Finite state machines** — a Kotlin DSL for statecharts; transitions are deterministic and pure
- 🧵 **Lifecycle-independent** — state logic lives outside composables; config changes don't lose it
- 🔁 **Persistent data structures** — structural sharing makes history cheap and diffs fast
- ⚙️ **Configuration-cache safe** — no reflection, no runtime surprises

## Modules

| Module | Purpose |
|---|---|
| `re-compose` | Core: state atom, event router, subscription graph |
| `fsm` | Kotlin DSL for finite state machines and statecharts |
| `http-fx` | HTTP effects (Ktor-based) |
| `paging-fx` | Paging integration for paginated domain data |

## Installation

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            name = "CentralPortalSnapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            content { includeGroup("io.github.yahyatinani.recompose") }
        }
    }
}
```

Then declare the dependency in your module's build.gradle.kts:

```kotlin
dependencies {
    implementation("io.github.yahyatinani.recompose:re-compose:X.Y.Z.N-SNAPSHOT")

    // Optional modules
    implementation("io.github.yahyatinani.recompose:fsm:X.Y.Z")
    implementation("io.github.yahyatinani.recompose:http-fx:X.Y.Z")
    implementation("io.github.yahyatinani.recompose:paging-fx:X.Y.Z")
}
```

Replace N with your snapshot build number and X.Y.Z with the matching release version when available.

Requirements: Kotlin 2.x, Jetpack Compose BOM 2025.x, minSdk 24, Gradle 9, AGP 9
