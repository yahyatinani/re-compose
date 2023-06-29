import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import io.github.yahyatinani.recompose.Ci

plugins {
  id("com.vanniktech.maven.publish.base")
}

extensions.configure<MavenPublishBaseExtension> {
  pom {
    val devUrl = "https://github.com/yahyatinani"
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
        id.set("yahyatinani")
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

  publishToMavenCentral(SonatypeHost.S01)
  if (Ci.isRelease || Ci.isSnapshot) {
    signAllPublications()
  }
  configure(AndroidSingleVariantLibrary())
  println("namename: $name")
  coordinates(
    groupId = Ci.groupId,
    artifactId = name,
    version = Ci.publishVersion
  )
}
