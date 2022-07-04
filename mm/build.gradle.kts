plugins {
    `java-library`
}

version = "0.0.1-SNAPSHOT"
base.archivesName.set("mm-lb")

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

dependencies {
    implementation(project(":lphybeast"))
    implementation(fileTree("lib"))
}

val developers = "LPhyBEAST developer team"
tasks.jar {
    manifest {
        // shared attr in the root build
        attributes(
            "Implementation-Title" to "LPhyBEAST morphological models",
            "Implementation-Vendor" to developers,
        )
    }
}
