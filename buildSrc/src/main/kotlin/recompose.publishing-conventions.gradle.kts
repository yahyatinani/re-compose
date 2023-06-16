import com.github.whyrising.recompose.Ci
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  id("com.vanniktech.maven.publish.base")
}

extensions.configure<MavenPublishBaseExtension> {
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

  publishToMavenCentral()
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
