rootProject.name = "re-compose"

dependencyResolutionManagement {
  versionCatalogs {
    create("deps") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}