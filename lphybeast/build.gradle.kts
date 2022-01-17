plugins {
    application
    distribution
}

group = "io.github.linguaphylo"
// version has to be manually adjusted to keep same between version.xml and here
version = "0.2-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
    withSourcesJar()
}

//val beast2 = files("lib/beast-2.6.6.jar")
//val beastPkgs = files("lib/BEASTlabs-1.9.7.jar","lib/BEAST_CLASSIC.addon.v1.5.0.jar",
//    "lib/FastRelaxedClockLogNormal.addon.v1.1.1.jar","lib/SSM.v1.1.0.jar","lib/feast-7.9.1.jar")
//val beastPkgs2 = files("lib/Mascot.v2.1.2.jar","lib/MM.addon.v1.1.1.jar","lib/SA.v2.0.2.jar")

dependencies {

    implementation("com.google.guava:guava:23.6-jre")
    implementation("org.jblas:jblas:1.2.3")
    implementation("info.picocli:picocli:4.6.2")

    // io.github.linguaphylo
    implementation("io.github.linguaphylo:lphy:1.1.0-SNAPSHOT")
//    implementation(project(mapOf( "path" to ":lphy", "configuration" to "coreJars")))

    // not released, so must include in lphybeast release
    implementation(files("lib/bdtree.jar"))
    // all released beast 2 libs
    implementation(fileTree("lib") {
        exclude("**/starbeast2-*.jar")
    })

    // tests
    testImplementation("junit:junit:4.13.2")

//    testRuntimeOnly(beast2)
//    testRuntimeOnly(beastPkgs)
//    testRuntimeOnly(beastPkgs2)
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

tasks.getByName<Tar>("distTar").enabled = false
// exclude start scripts
tasks.getByName<CreateStartScripts>("startScripts").enabled = false

// dist as a beast2 package, so all released b2 packages are excluded.
distributions {
    main {
        contents {
            // include src jar
            from(layout.buildDirectory.dir("libs")) {
                include("*-sources.jar")
                into("src")
            }
            from("$rootDir") {
                include("README.md")
                include("version.xml")
            }
            // TODO better solution?
            exclude("**/beast-*.jar", "**/BEAST*.jar", "**/*addon*.jar",
                "**/feast-*.jar", "**/SSM.*.jar", "**/SA.*.jar", "**/Mascot.*.jar")
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

