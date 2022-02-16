plugins {
    `java-library`
}

version = "0.0.1-SNAPSHOT"
base.archivesName.set("mascot-lb")

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
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
            "Implementation-Title" to "LPhyBEAST Mascot",
            "Implementation-Vendor" to developers,
        )
    }
}

// launch lphybeast
tasks.register("runLPhyBEAST", JavaExec::class.java) {
    // use classpath
    jvmArgs = listOf("-cp", sourceSets.main.get().runtimeClasspath.asPath)
    println("clspath = ${sourceSets.main.get().runtimeClasspath.asPath}")
    mainClass.set("lphybeast.LPhyBEAST")
    setArgs(listOf("-o", "$rootDir/../linguaPhylo/tmp/h3n2.xml",
            "$rootDir/../linguaPhylo/tutorials/h3n2.lphy"))
    println("args = $args")
}


