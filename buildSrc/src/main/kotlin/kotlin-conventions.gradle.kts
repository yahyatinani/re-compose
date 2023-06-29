import io.github.yahyatinani.recompose.Ci
import io.github.yahyatinani.recompose.Versions
import io.github.yahyatinani.recompose.Versions.KOTLIN
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("android")
}

version = Ci.publishVersion

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
      TestLogEvent.STANDARD_ERROR
    )
  }
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
    jvmTarget = Versions.JVM
    apiVersion = KOTLIN
    languageVersion = KOTLIN
  }
}

kotlin {
  sourceSets {
    all {
      languageSettings.optIn("kotlin.time.ExperimentalTime")
      languageSettings.optIn("kotlin.experimental.ExperimentalTypeInference")
      languageSettings.optIn("kotlin.contracts.ExperimentalContracts")
    }
  }
}
