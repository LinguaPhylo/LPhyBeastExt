plugins {
    `java-library`
    `java-test-fixtures` // which produces test fixtures
    distribution
    `maven-publish`
    signing
    id("io.github.linguaphylo.platforms.lphy-publish") version "0.1.2"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
tasks.jar.get().enabled = false
tasks.testFixturesJar.get().enabled = false

version = "0.1.0-SNAPSHOT"
//base.archivesName.set("LPhyBeastExt")

//val lbver = "0.4.0-SNAPSHOT"
val zippedConfig by configurations.creating

dependencies {
    api("io.github.linguaphylo:lphy:1.3.0")
    // BEAST2
    api(fileTree("lib2"))

    //*** lphybeast + ... ***//
    // cannot use "version" in zippedConfig
    zippedConfig("io.github.linguaphylo:lphybeast:0.4.0-SNAPSHOT")
//    implementation(fileTree("dir" to "${lb.get().outputs.dir("lib")}", "include" to "**/*.jar"))
    api(files( { lb.get().extra["lblibs"] } ))

    api(fileTree("lib-test"))
    // test api not working
//    testFixturesApi("org.junit.jupiter:junit-jupiter:5.8.2")
//    testImplementation(fileTree("$rootDir/lib-test"))
//    testFixturesApi("io.github.linguaphylo:lphybeast:$lbver")
//    testImplementation(testFixtures("io.github.linguaphylo:lphybeast:$lbver"))
}

tasks.compileJava.get().dependsOn("installLPhyBEAST")

// unzip lphybeast-*.zip to ${buildDir}/lphybeast/
val lb = tasks.register<Sync>("installLPhyBEAST") {
    val outDir = "${buildDir}/lphybeast"
    zippedConfig.resolvedConfiguration.resolvedArtifacts.forEach({
        println(name + " --- " + it.file.name)
        if (it.file.name.endsWith("zip")) {
            from(zipTree(it.file))
            into(outDir)
        }
    })
    extra["lblibs"] = fileTree("dir" to "${outDir}/lib", "include" to "**/*.jar")
}

//++++++++ release ++++++++//
// It needs to run ./gradlew clean build to build all subprojects,
// and then ./gradlew build -x test again, to create zip having contents from other subprojects

tasks.getByName<Tar>("distTar").enabled = false
// exclude start scripts
//tasks.getByName<CreateStartScripts>("startScripts").enabled = false

// dist as a beast2 package:
// 1. never use `application` Gradle plugin;
// 2. include the task output, not the output files;
// 3. exclude lphy from lphybeast core release, because SPI does not work with BEAST class loader.
//    But for lphybeast extensions, lphy part has to be included, due to BEAST class loader.
distributions {
    main {
        distributionBaseName.set("LPhyBeastExt")
        contents {
//            eachFile {  println(relativePath)  }
            includeEmptyDirs = false
            into("lib") {
//                println(lblibs.files.toList())
                from(project(":mascot").layout.buildDirectory.dir("libs")){
                    exclude("*-sources.jar")
                }
                from(project(":mm").layout.buildDirectory.dir("libs")){
                    exclude("*-sources.jar")
                }
                from(project(":sa").layout.buildDirectory.dir("libs")){
                    exclude("*-sources.jar")
                }
            }
            into("."){
                from("$rootDir") {
                    include("README.md")
                    include("LICENSE")
                    include("version.xml")
                }
            }
            // include src jar
            into("src") {
                from(project(":mascot").layout.buildDirectory.dir("libs")){
                    include("*-sources.jar")
                }
                from(project(":mm").layout.buildDirectory.dir("libs")){
                    include("*-sources.jar")
                }
                from(project(":sa").layout.buildDirectory.dir("libs")){
                    include("*-sources.jar")
                }
            }
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

val developers = "LPhyBEAST developer team"
val webSteam = "github.com/LinguaPhylo/LPhyBeastExt"
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
                    "An extension of LPhyBEAST."
                )
                // compulsory
                url.set("https://linguaphylo.github.io/")
                packaging = "zip"
                developers {
                    developer {
                        name.set(developers)
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


