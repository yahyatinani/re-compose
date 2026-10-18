// buildSrc is JVM code compiled by Gradle's daemon (JDK 17+); java.util.Optional
// is always available here. Android Studio's lint applies minSdk rules to buildSrc
// anyway — known IDE limitation, not a real API-level issue.
@file:Suppress("NewApi")

package io.github.yahyatinani.recompose

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.getByType

class Ci(private val providers: ProviderFactory, private val snapshotBase: String) {
  val groupId = "io.github.yahyatinani.recompose"

  private val buildNumber = providers.environmentVariable("GITHUB_RUN_NUMBER")
  private val releaseVersion = providers.environmentVariable("RELEASE_VERSION")

  val isRelease: Boolean get() = releaseVersion.isPresent

  val publishVersion: String
    get() = releaseVersion.orNull
      ?: buildNumber.orNull?.let { "$snapshotBase.$it-SNAPSHOT" }
      ?: "$snapshotBase-LOCAL"

  val isSnapshot: Boolean get() = publishVersion.endsWith("SNAPSHOT")
}

fun Project.ci(): Ci = Ci(
  providers = providers,
  snapshotBase = extensions
    .getByType<VersionCatalogsExtension>()
    .named("deps")
    .findVersion("recompose")
    .get()
    .requiredVersion,
)
