plugins {
  `kotlin-dsl`
}

repositories {
  mavenCentral()
  google()
  gradlePluginPortal()
}

dependencies {
  implementation(deps.android.gradle.plugin)
  implementation(deps.android.library.gradle.plugin)
}
