package io.github.yahyatinani.recompose

object Ci {
  const val groupId = "io.github.yahyatinani.recompose"
  private const val snapshotBase = "0.4.0"

  private fun githubBuildNumber() = System.getenv("GITHUB_RUN_NUMBER")

  private fun snapshotVersion(): String = when (val n = githubBuildNumber()) {
    null -> "$snapshotBase-LOCAL"
    else -> "$snapshotBase.$n-SNAPSHOT"
  }

  private fun releaseVersion() = System.getenv("RELEASE_VERSION")

  val isRelease get() = releaseVersion() != null

  val isSnapshot get() = snapshotVersion().endsWith("SNAPSHOT")

  val publishVersion: String = releaseVersion() ?: snapshotVersion()
}
