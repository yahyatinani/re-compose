import com.github.whyrising.recompose.Ci

plugins {
  id("recompose.android-lib-conventions")
  signing
  `maven-publish`
}

val ossrhUsername: String by project
val ossrhPassword: String by project
val signingKey: String? by project
val signingPassword: String? by project

publishing {
  repositories {
    maven {
      val host = "https://oss.sonatype.org"
      val releasesRepoUrl =
        uri("$host/service/local/staging/deploy/maven2/")
      val snapshotsRepoUrl = uri("$host/content/repositories/snapshots/")

      name = "deploy"
      url = when {
        Ci.isRelease -> releasesRepoUrl
        else -> snapshotsRepoUrl
      }
      credentials {
        username = System.getenv("OSSRH_USERNAME") ?: ossrhUsername
        password = System.getenv("OSSRH_PASSWORD") ?: ossrhPassword
      }
    }
  }

  publications {
    register<MavenPublication>("release") {
      groupId = Ci.groupId
      artifactId = Ci.artifactId
      version = Ci.publishVersion

      pom {
        val devUrl = "https://github.com/whyrising"
        val libUrl = "$devUrl/re-compose"

        name.set("Re-compose")
        description.set("Event Driven Android UI Framework")
        url.set(libUrl)

        licenses {
          license {
            name.set("GPL-3.0")
            url.set("https://opensource.org/licenses/gpl-3.0")
          }
        }

        developers {
          developer {
            id.set("whyrising")
            name.set("Yahya Tinani")
            email.set("yahyatinani@gmail.com")
          }
        }

        scm {
          connection.set("scm:git:$libUrl")
          developerConnection.set("scm:git:$devUrl")
          url.set(libUrl)
        }
      }

      afterEvaluate {
        from(components["release"])
      }
    }
  }
}

val publications: PublicationContainer =
  (extensions.getByName("publishing") as PublishingExtension).publications

signing {
  useGpgCmd()
  if (signingKey != null && signingPassword != null) {
    useInMemoryPgpKeys(signingKey, signingPassword)
  }

  if (Ci.isRelease) {
    sign(publications)
  }
}
