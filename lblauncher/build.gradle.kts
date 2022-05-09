plugins {
    application
//    distribution
    `maven-publish`
    signing
//    id("io.github.linguaphylo.platforms.lphy-java") version "0.1.2"
    id("io.github.linguaphylo.platforms.lphy-publish") version "0.1.2"
}

//version in root build
base.archivesName.set("lphy-beast-app")

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withJavadocJar()
    withSourcesJar()
}

dependencies {

    implementation("io.github.linguaphylo:lphy-studio:1.3.0-SNAPSHOT")

    implementation(project(":lphybeast"))
    // BEAST launcher
    implementation(fileTree("lib"))

}

val maincls : String = "lphystudio.app.LinguaPhyloStudio"
//application {
//    // equivalent to -m lphystudio
//    // need both mainModule and mainClass
//    mainModule.set("lphystudio")
//    // if only mainClass, it will auto add maincls to the end of CMD
//    mainClass.set(maincls)
//}

// make studio app locating the correct parent path of examples sub-folder
tasks.withType<JavaExec>() {
    // set version into system property
    systemProperty("lphy.beast.version", version)

    doFirst {
        // equivalent to: java -p ...
        jvmArgs = listOf("-p", classpath.asPath)
        classpath = files()
    }
    doLast {
        println("JavaExec : $jvmArgs")
    }
}

val developers = "LPhyBEAST developer team"
tasks.jar {
    manifest {
        // shared attr in the root build
        attributes(
            "Main-Class" to maincls,
            "Implementation-Title" to "LPhyBEAST",
            "Implementation-Vendor" to developers,
        )
    }
}

//publishing {
//    publications {
//        // project.name contains "lphy" substring
//        create<MavenPublication>(project.name) {
//            artifactId = project.base.archivesName.get()
//            pom {
//                description.set("The GUI for LPhy language.")
//                developers {
//                    developer {
//                        name.set(developers)
//                    }
//                }
//            }
//        }
//
//    }
//}
//
