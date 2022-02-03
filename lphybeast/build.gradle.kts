plugins {
    `java-library`
    application
    distribution
    `maven-publish`
    signing
    id("io.github.linguaphylo.platforms.lphy-publish") version "0.1.1"
}

group = "io.github.linguaphylo"
// TODO 3 versions: here, LPhyBEAST, version.xml
// version has to be manually adjusted to keep same between version.xml and here
version = "0.2.1"

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
    withSourcesJar()
}

dependencies {
    implementation("com.google.guava:guava:23.6-jre")
    implementation("org.jblas:jblas:1.2.3")
    implementation("info.picocli:picocli:4.6.2")

    // io.github.linguaphylo
//    implementation("io.github.linguaphylo:lphy:1.1.0-SNAPSHOT")
//    implementation(project(mapOf( "path" to ":lphy", "configuration" to "coreJars")))
    implementation(fileTree("lib") {
        // non-modular lphy jar incl. all dependencies
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

var maincls: String = "lphybeast.LPhyBEAST"
application {
    // use classpath, beast2 not use module
    mainClass.set(maincls)
}

tasks.withType<JavaExec>() {
    // set version into system property
    systemProperty("lphy.beast.version", version)
//    systemProperty("user.dir", rootDir)
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
        from(rootDir) {
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
//            eachFile {  println(relativePath)  }
            includeEmptyDirs = false
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

// beast 2 will remove version from Zip file name, and then decompress
// rm lphybeast-$version from the relative path of files inside Zip to make it working
tasks.withType<Zip>() {
    doFirst {
        if ( name.equals("distZip") ) {
            // only activate in distZip, otherwise will affect all jars and zips,
            // e.g. main class not found in lphybeast-$version.jar.
            eachFile {
                relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
                println(relativePath)
            }
        }
    }
}

val webSteam = "github.com/LinguaPhylo/LPhyBeast"
publishing {
    publications {
        // project.name contains "lphy" substring
        create<MavenPublication>(project.name) {
            artifactId = project.base.archivesName.get()
            artifact(tasks.distZip.get())
            // Configures the version mapping strategy
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set(project.name)
                description.set(
                    "A command-line program that takes an LPhy model specification " +
                            "including a data block, and produces a BEAST 2 XML input file."
                )
                // compulsory
                url.set("https://linguaphylo.github.io/")
                packaging = "zip"
                developers {
                    developer {
                        name.set("Alexei Drummond and Walter Xie")
                    }
                }
                properties.set(
                    mapOf(
                        "maven.compiler.source" to java.sourceCompatibility.majorVersion,
                        "maven.compiler.target" to java.targetCompatibility.majorVersion
                    )
                )
                licenses {
                    license {
                        name.set("GNU Lesser General Public License, version 3")
                        url.set("https://www.gnu.org/licenses/lgpl-3.0.txt")
                    }
                }
                // https://central.sonatype.org/publish/requirements/
                scm {
                    connection.set("scm:git:git://${webSteam}.git")
                    developerConnection.set("scm:git:ssh://${webSteam}.git")
                    url.set("https://${webSteam}")
                }
            }
            println("Define MavenPublication ${name} and set shared contents in POM")
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

