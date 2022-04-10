apply(plugin = "maven-publish")
apply(plugin = "signing")

fun Project.publishing(action: PublishingExtension.() -> Unit) =
    configure(action)

fun Project.signing(configure: SigningExtension.() -> Unit): Unit =
    configure(configure)

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
                com.github.whyrising.recompose.Ci.isRelease() -> releasesRepoUrl
                else -> snapshotsRepoUrl
            }
            credentials {
                username = System.getenv("OSSRH_USERNAME") ?: ossrhUsername
                password = System.getenv("OSSRH_PASSWORD") ?: ossrhPassword
            }
        }
    }
}

val publications: PublicationContainer =
    (extensions.getByName("publishing") as PublishingExtension).publications

signing {
    useGpgCmd()
    if (signingKey != null && signingPassword != null)
        useInMemoryPgpKeys(signingKey, signingPassword)

    if (com.github.whyrising.recompose.Ci.isRelease())
        sign(publications)
}
