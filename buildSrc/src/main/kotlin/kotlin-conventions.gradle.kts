import io.github.yahyatinani.recompose.ci
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
  // REMOVE: id("org.jetbrains.kotlin.android")
  // The kotlin-dsl helper in buildSrc/build.gradle.kts already provides
  // precompiled script compilation support.
}

val ci = ci()

version = ci.publishVersion

tasks.withType<Test> {
  useJUnitPlatform()
  val decimal = Runtime.getRuntime().availableProcessors() / 2
  maxParallelForks = if (decimal > 0) decimal else 1
  filter {
    isFailOnNoMatchingTests = false
  }
  testLogging {
    exceptionFormat = TestExceptionFormat.FULL
    events = setOf(
      TestLogEvent.SKIPPED,
      TestLogEvent.FAILED,
      TestLogEvent.STANDARD_OUT,
      TestLogEvent.STANDARD_ERROR,
    )
  }
}

// tasks.withType<KotlinCompile>().configureEach {
//  compilerOptions {
//    freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
//    jvmTarget = Versions.JVM
//    apiVersion = KOTLIN
//    languageVersion = KOTLIN
//  }
// }

// kotlin {
//  sourceSets {
//    all {
//      languageSettings.optIn("kotlin.time.ExperimentalTime")
//      languageSettings.optIn("kotlin.experimental.ExperimentalTypeInference")
//      languageSettings.optIn("kotlin.contracts.ExperimentalContracts")
//    }
//  }
// }
