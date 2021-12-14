// A standalone project

// Configures this project and each of its sub-projects.
allprojects {
    repositories {
        mavenCentral()
        // add sonatype snapshots repository
        maven {
            url=uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        }
//        mavenLocal() // only for testing
    }
}
