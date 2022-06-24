package com.github.whyrising.recompose

object Ci {
  private const val snapshotBase = "0.0.6"

  private fun githubBuildNumber() = System.getenv("GITHUB_RUN_NUMBER")

  private fun snapshotVersion(): String = when (val n = githubBuildNumber()) {
    null -> "$snapshotBase-LOCAL"
    else -> "$snapshotBase.$n-SNAPSHOT"
  }

  private fun releaseVersion() = System.getenv("RELEASE_VERSION")

  val isRelease get() = releaseVersion() != null

  val publishVersion: String = releaseVersion() ?: snapshotVersion()

  val groupId = "com.github.whyrising.recompose"
  val artifactId = "recompose"
}
