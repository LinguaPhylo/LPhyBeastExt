plugins {
    application
    distribution
}

group = "io.github.linguaphylo"
version = "0.2-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
    withSourcesJar()
}

val beast2 = files("lib/beast-2.6.6.jar")
val beastPkgs = files("lib/BEASTlabs-1.9.7.jar","lib/BEAST_CLASSIC.addon.v1.5.0.jar",
    "lib/FastRelaxedClockLogNormal.addon.v1.1.1.jar","lib/SSM.v1.1.0.jar","lib/feast-7.9.1.jar")
val beastPkgs2 = files("lib/Mascot.v2.1.2.jar","lib/MM.addon.v1.1.1.jar","lib/SA.v2.0.2.jar")

dependencies {

    implementation("com.google.guava:guava:23.6-jre")
    implementation("org.jblas:jblas:1.2.3")
    implementation("info.picocli:picocli:4.5.2")

    // io.github.linguaphylo
    implementation("io.github.linguaphylo:lphy:1.1.0-SNAPSHOT")
//    implementation(project(mapOf( "path" to ":lphy", "configuration" to "coreJars")))

    // not released, so must include in lphybeast release
    implementation(files("lib/bdtree.jar"))
    // TODO compileOnly not working, probably BEAST classes did not load correctly
    // beast 2 libs
    implementation(beast2)
    implementation(beastPkgs)
    implementation(beastPkgs2)

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
    manifest {
        // shared attr in the root build
        attributes(
            "Main-Class" to maincls,
            "Implementation-Title" to "LPhyBEAST"
        )
    }
    // copy LICENSE to META-INF
    metaInf {
        from (rootDir) {
            include("LICENSE")
        }
    }
}


//TODO dist beast2 package
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

