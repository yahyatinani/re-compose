package com.github.whyrising.recompose

object Ci {
  const val groupId = "com.github.whyrising.recompose"
  const val artifactId = "recompose"

  private const val snapshotBase = "0.3.0"

  private fun githubBuildNumber() = System.getenv("GITHUB_RUN_NUMBER")

  private fun snapshotVersion(): String = when (val n = githubBuildNumber()) {
    null -> "$snapshotBase-LOCAL"
    else -> "$snapshotBase.$n-SNAPSHOT"
  }

  private fun releaseVersion() = System.getenv("RELEASE_VERSION")

  val isRelease get() = releaseVersion() != null

  val publishVersion: String = releaseVersion() ?: snapshotVersion()
}
