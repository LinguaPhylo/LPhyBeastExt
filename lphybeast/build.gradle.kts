import java.text.SimpleDateFormat
import java.util.Calendar

plugins {
    application
    distribution
    `maven-publish`
    signing
    id("io.github.linguaphylo.platforms.lphy-java") version "0.1.1"
    id("io.github.linguaphylo.platforms.lphy-publish") version "0.1.1"
}

group = "io.github.linguaphylo"
version = "0.2-SNAPSHOT"

val beast2 = files("lib/beast-2.6.6.jar")
val beastLabs = files("lib/BEASTlabs-1.9.7.jar")
val beastClsc = files("lib/BEAST_CLASSIC.addon.v1.5.0.jar")
val fastRlxClkLN = files("lib/FastRelaxedClockLogNormal.addon.v1.1.1.jar")
val ssm = files("lib/SSM.v1.1.0.jar")
val feast = files("lib/feast-7.9.1.jar")
val mascot = files("lib/Mascot.v2.1.2.jar")
val mm = files("lib/MM.addon.v1.1.1.jar")
val sa = files("lib/SA.v2.0.2.jar")
// not released
val bdtree = files("lib/bdtree.jar")

dependencies {

    implementation("com.google.guava:guava:23.6-jre")
    implementation("org.jblas:jblas:1.2.3")
    implementation("info.picocli:picocli:4.5.2")

    // io.github.linguaphylo
    implementation("io.github.linguaphylo:lphy:1.1.0-SNAPSHOT")
//    implementation(project(mapOf( "path" to ":lphy", "configuration" to "coreJars")))

    // implementation will include jars during distribution
    // beast 2 libs
    implementation(beast2) // compileOnly(project(":beast2"))
    implementation(beastLabs)
    implementation(beastClsc)
    implementation(fastRlxClkLN)
    implementation(ssm)
    implementation(feast)
    implementation(mascot)
    implementation(mm)
    implementation(sa)
    implementation(bdtree) // must include in lphybeast release

    // tests
    testImplementation("junit:junit:4.13.2")

//    testRuntimeOnly(beast2)
//    testRuntimeOnly(beastLabs)
//    testRuntimeOnly(beastClsc)
//    testRuntimeOnly(fastRlxClkLN)
//    testRuntimeOnly(ssm)
//
//    testRuntimeOnly(feast)
//    testRuntimeOnly(mascot)
//    testRuntimeOnly(mm)
//    testRuntimeOnly(sa)

}

var maincls : String = "lphybeast.LPhyBEAST"
application {
    // use classpath, beast2 not use module
    mainClass.set(maincls)
}

tasks.withType<JavaExec>() {
    // set version into system property
    systemProperty("lphy.beast.version", version)
    systemProperty("user.dir", rootDir)
}


tasks.jar {
    var calendar: Calendar? = Calendar.getInstance()
    var formatter = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss")

    manifest {
        // shared attr in the root build
        attributes(
            "Main-Class" to maincls,
            "Implementation-Title" to "LPhyBEAST",
            "Implementation-Vendor" to "Alexei Drummond and Walter Xie",
            "Implementation-Version" to archiveVersion,
            "Implementation-URL" to "https://github.com/LinguaPhylo/LPhyBeast/",
            "Built-By" to "Walter Xie", //System.getProperty("user.name"),
            "Build-Jdk" to JavaVersion.current().majorVersion.toInt(),
            "Built-Date" to formatter.format(calendar?.time)
        )
    }
    // copy LICENSE to META-INF
    metaInf {
        from (rootDir) {
            include("LICENSE")
        }
    }
}

publishing {
    publications {
        // project.name contains "lphy" substring
        create<MavenPublication>(project.name) {
            artifactId = project.base.archivesName.get()
            pom {
                description.set("The GUI for LPhy language.")
                developers {
                    developer {
                        name.set("Alexei Drummond")
                    }
                    developer {
                        name.set("Walter Xie")
                    }
                }
            }
        }

    }
}



//TODO
distributions {
    main {
        contents {
            from("$rootDir") {
                include("README.md")
                include("version.xml")
            }
            // include src jar
            from(layout.buildDirectory.dir("libs")) {
                include("*-sources.jar")
                into("src")
            }
        }
    }
}


tasks.test {
    useJUnit()
    // useJUnitPlatform()
    // set heap size for the test JVM(s)
    minHeapSize = "128m"
    maxHeapSize = "1G"
    // show standard out and standard error of the test JVM(s) on the console
    testLogging.showStandardStreams = true
    //testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    exclude("**/Tutorial*")
}

