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
//tasks.testFixturesJar.get().enabled = false

version = "0.3.1-SNAPSHOT" // -SNAPSHOT
base.archivesName.set("LPhyBeastExt")

val zippedConfig by configurations.creating

val beast2Jars = fileTree("lib") {
    exclude("**/*-sources.jar")
}

val outDir = "${buildDir}/lphybeast"

dependencies {
    /**
     * The behaviour of this default version declaration chooses any available highest version first.
     * If the exact version is required, then use the "strictly version" declaration
     * such as "io.github.linguaphylo:lphy:1.2.0!!".
     * https://docs.gradle.org/current/userguide/rich_versions.html#sec:strict-version
     */
    api("io.github.linguaphylo:lphy:1.5.0-SNAPSHOT") //-SNAPSHOT
    api("io.github.linguaphylo:lphy-base:1.5.0-SNAPSHOT")
    // BEAST2
    api(beast2Jars)

    //*** lphybeast + ... ***//
    // cannot use "version" in zippedConfig
    zippedConfig("io.github.linguaphylo:lphybeast:1.1.0-SNAPSHOT")
    // it must run installLPhyBEAST to unzip lphybeast.zip and create ${outDir}/lib,
    // the build is cleaned, or lphybeast version is renewed.
    api(fileTree("dir" to "${outDir}/lib", "include" to "**/*.jar"))

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
    zippedConfig.resolvedConfiguration.resolvedArtifacts.forEach({
        println(name + " --- " + it.file.name)
        if (it.file.name.endsWith("zip")) {
            from(zipTree(it.file))
            // why zipTree cannot provide files in root
            from(zipTree(it.file).matching({ include("**/version.xml") }).singleFile)
            into(outDir)
        }
    })
//    extra["lblibs"] = fileTree("dir" to "${outDir}/lib", "include" to "**/*.jar")
//    extra["lbsrc"] = fileTree("dir" to "${outDir}/src", "include" to "**/*-sources.jar")
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
        distributionBaseName.set(project.base.archivesName.get())
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


