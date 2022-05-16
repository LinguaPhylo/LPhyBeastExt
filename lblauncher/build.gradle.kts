plugins {
    `java-library`
    `maven-publish`
    signing
    id("io.github.linguaphylo.platforms.lphy-publish") version "0.1.2"
}

//version in root build
base.archivesName.set("lb-launcher")

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation("io.github.linguaphylo:lphy:1.3.0-SNAPSHOT")
    implementation("io.github.linguaphylo:lphy-studio:1.3.0-SNAPSHOT")
    implementation("io.github.linguaphylo:ext-manager:0.1.0-SNAPSHOT")

    implementation(project(":lphybeast")) // not depend on LPhyBeast, only use for debug
    // BEAST launcher
    implementation(fileTree("lib"))

}

val maincls : String = "lphybeast.launcher.LPhyBeastLauncher"

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
