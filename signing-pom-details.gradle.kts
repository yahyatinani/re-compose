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

val publications: PublicationContainer =
    (extensions.getByName("publishing") as PublishingExtension).publications

signing {
    useGpgCmd()
    if (signingKey != null && signingPassword != null) {
        @Suppress("UnstableApiUsage")
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
    if (Ci.isRelease()) {
        sign(publications)
    }
}

publishing {
    repositories {
        maven {

            val host = "https://oss.sonatype.org"
            val releasesRepoUrl =
                uri("$host/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("$host/content/repositories/snapshots/")

            name = "deploy"
            url = if (Ci.isRelease()) releasesRepoUrl else snapshotsRepoUrl
            credentials {
                username = System.getenv("OSSRH_USERNAME") ?: ossrhUsername
                password = System.getenv("OSSRH_PASSWORD") ?: ossrhPassword
            }
        }
    }
}
