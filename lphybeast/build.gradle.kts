import java.nio.file.*

plugins {
    application
    `maven-publish`
}

group = "lphybeast"
version = "0.2-SNAPSHOT"


dependencies {
    implementation(project(":lphy"))
    //TODO
//    implementation(files("libs/lphy-1.1-SNAPSHOT.jar"))
//    implementation(files("$projectDir/../linguaPhylo/lphy/libs/jebl-3.0.1.jar"))

    implementation("com.google.guava:guava:23.6-jre")
    implementation("org.jblas:jblas:1.2.3")
    implementation("info.picocli:picocli:4.5.2")

    // not in maven
    implementation(files("libs/beast-2.6.6.jar"))
    implementation(files("libs/BEASTlabs-1.9.7.jar"))
    implementation(files("libs/BEAST_CLASSIC.addon.v1.5.0.jar"))
    implementation(files("libs/FastRelaxedClockLogNormal.addon.v1.1.1.jar"))
    implementation(files("libs/SSM.v1.1.0.jar"))

    implementation(files("libs/bdtree.jar"))
    implementation(files("libs/feast-7.9.1.jar"))
    implementation(files("libs/Mascot.v2.1.2.jar"))
    implementation(files("libs/MM.addon.v1.1.1.jar"))
    implementation(files("libs/SA.v2.0.2.jar"))
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
    maxHeapSize = "1G"

    exclude("**/Tutorial*")
}


tasks.register("showCache") {
    doLast {
        configurations.compileClasspath.get().forEach { println(it) }
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

