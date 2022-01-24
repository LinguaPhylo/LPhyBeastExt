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

// TODO 3 versions: here, LPhyBEAST, version.xml
var lphyVersion = "1.1.0-SNAPSHOT"

dependencies {
    implementation("com.google.guava:guava:23.6-jre")
    implementation("org.jblas:jblas:1.2.3")
    implementation("info.picocli:picocli:4.6.2")

    // io.github.linguaphylo
//    implementation("io.github.linguaphylo:lphy:${lphyVersion}")
//    implementation(project(mapOf( "path" to ":lphy", "configuration" to "coreJars")))
    implementation(fileTree("lib") {
        include("lphy-*-all.jar")
        // not released, so must include in lphybeast release
        include("bdtree.jar")
    })

    // all released beast 2 libs
    implementation(fileTree("lib2") {
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

// dist as a beast2 package:
// 1. all released b2 packages are excluded;
// 2. lphy-*-all.jar is excluded, because SPI is not working with BEAST;
// 3. cannot use modular jar, because BEAST uses a customised non-module system.
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
            from("${layout.projectDirectory.dir("bin")}") {
                include("lphybeast")
                into("bin")
                eachFile {
                    // fileMode 755 not working
                    file.setExecutable(true, true)
                }
            }
            // TODO better solution?
            exclude("**/beast-*.jar", "**/BEAST*.jar", "**/*addon*.jar",
                "**/feast-*.jar", "**/SSM.*.jar", "**/SA.*.jar", "**/Mascot.*.jar")
//            exclude(fileTree("lib2").toList().map(File::getAbsolutePath)) // not working
            // SPI not working in BEAST2
            exclude("**/lphy-*-all.jar")
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

