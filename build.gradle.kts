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

subprojects {
    group = "io.github.linguaphylo"
    // TODO 3 versions: here, LPhyBEAST, version.xml
    // version has to be manually adjusted to keep same between version.xml and here
    val webSteam = "github.com/LinguaPhylo/LPhyBeastExt"
    val web = "https://${webSteam}"

    var calendar: java.util.Calendar? = java.util.Calendar.getInstance()
    var formatter = java.text.SimpleDateFormat("dd-MMM-yyyy HH:mm:ss")

    // shared attributes
    tasks.withType<Jar>() {
        // this includes pom.xml in the jar
//        dependsOn(copyPom)

        manifest {
            attributes(
                "Implementation-Version" to archiveVersion,
                "Implementation-URL" to web,
                "Built-By" to "Walter Xie", //System.getProperty("user.name"),
                "Build-Jdk" to JavaVersion.current().majorVersion.toInt(),
                "Built-Date" to formatter.format(calendar?.time)
            )
        }
        // copy LICENSE to META-INF
        metaInf {
            from(rootDir) {
                include("README.md")
                include("LICENSE")
            }
        }

    }

}
