import java.nio.file.*

plugins {
    application
    distribution
    `maven-publish`
    signing
}

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

// configure core dependencies, which can be reused
val coreJars by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    extendsFrom(configurations["implementation"])
}
// Attach the task jar to an outgoing configuration coreJars
artifacts {
    add("coreJars", tasks.jar)
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
    //withJavadocJar()
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


