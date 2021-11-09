import java.nio.file.*

plugins {
    application
    `maven-publish`
}

group = "lphybeast"
version = "0.2-SNAPSHOT"


dependencies {
    implementation(project(mapOf( "path" to ":lphy", "configuration" to "coreJars"))) // compileOnly
    //TODO
//    implementation(files("lib/lphy-1.1-SNAPSHOT.jar"))
//    implementation(files("$projectDir/../linguaPhylo/lphy/lib/jebl-3.0.1.jar"))
    implementation("com.google.guava:guava:23.6-jre")
    implementation("org.jblas:jblas:1.2.3")
    implementation("info.picocli:picocli:4.5.2")

    // not in maven
    implementation(files("lib/beast-2.6.6.jar")) // compileOnly
//    compileOnly(project(":beast2"))
    implementation(files("lib/BEASTlabs-1.9.7.jar"))
    implementation(files("lib/BEAST_CLASSIC.addon.v1.5.0.jar"))
    implementation(files("lib/FastRelaxedClockLogNormal.addon.v1.1.1.jar"))
    implementation(files("lib/SSM.v1.1.0.jar"))

    implementation(files("lib/feast-7.9.1.jar"))
    implementation(files("lib/Mascot.v2.1.2.jar"))
    implementation(files("lib/MM.addon.v1.1.1.jar"))
    implementation(files("lib/SA.v2.0.2.jar"))
    // not released
    implementation(files("lib/bdtree.jar"))

    //implementation(fileTree("lib") { exclude("junit-*.jar") })

    testImplementation("junit:junit:4.13.2")
}

var maincls : String = "lphybeast.LPhyBEAST"
application {
//    mainModule.set("lphystudio")
    mainClass.set(maincls)
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
    withSourcesJar()
}

tasks.jar {
    manifest {
        // shared attr in the root build
        attributes(
            "Main-Class" to maincls,
            "Implementation-Title" to "LPhyBEAST",
        )
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

distributions {
    main {
        contents {
            from("$rootDir") {
                include("README.md")
            }
        }
    }
}


//val releaseDir = "releases"
//tasks.withType<AbstractPublishToMaven>().configureEach {
//    doFirst {
//        val path: java.nio.file.Path = Paths.get("${rootDir}", releaseDir)
//        if (Files.exists(path)) {
//            println("Delete the existing previous release : ${path.toAbsolutePath()}")
//            project.delete(path)
//        }
//    }
//}

//publishing {
//    publications {
//        create<MavenPublication>("LPhyBEAST") {
//            artifactId = "core"
//            from(components["java"])
//        }
//    }
//
//    repositories {
//        maven {
//            name = releaseDir
//            url = uri(layout.buildDirectory.dir("${rootDir}/${releaseDir}"))
//            println("Set the base URL of $releaseDir repository to : ${url.path}")
//        }
//    }
//}

