plugins {
    application
    distribution
//    `maven-publish`
//    signing
//    id("io.github.linguaphylo.platforms.lphy-publish") version "0.1.1"
}

version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
    withSourcesJar()
}

dependencies {
    implementation(project(mapOf( "path" to ":lphybeast")))

    implementation(files("lib/launcher-2.6.6.jar"))
}

var maincls : String = "lphybeast.app.LPhyBEASTLauncher"
application {
    mainClass.set(maincls)
}

// make studio app locating the correct parent path of examples sub-folder
tasks.withType<JavaExec>() {
    // set version into system property
    systemProperty("lphy.beast.version", version)
    // rootDir = projectDir.parent = ~/WorkSpace/LPhyBeast
    systemProperty("user.dir", rootDir)
}

tasks.jar {
    manifest {
        // shared attr in the root build
        attributes(
            "Main-Class" to maincls,
            "Implementation-Title" to "LPhyBEAST App"
        )
    }
}


distributions {
    main {
        contents {
            // include src jar
            from(layout.buildDirectory.dir("libs")) {
                include("*-sources.jar")
                into("src")
            }
        }
    }
}
