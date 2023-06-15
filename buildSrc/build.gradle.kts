plugins {
  `kotlin-dsl`
}

repositories {
  mavenCentral()
  google()
  gradlePluginPortal()
}

dependencies {
  implementation(deps.gradlePlugin.android)
  implementation(deps.gradlePlugin.kotlin)
//  implementation(deps.gradlePlugin.mavenPublish)
}
