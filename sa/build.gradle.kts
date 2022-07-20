plugins {
    `java-library`
//    `java-test-fixtures` // which produces test fixtures
}

version = "0.0.1-SNAPSHOT"
base.archivesName.set("sa-lb")

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

val zippedConfig by configurations.creating
dependencies {
    implementation("io.github.linguaphylo:lphy:1.3.0")

    implementation(fileTree("$rootDir/lib2"))
    implementation(fileTree("lib"))

    //*** lphybeast + ... ***//
    zippedConfig("io.github.linguaphylo:lphybeast:0.4.0-SNAPSHOT")
//    implementation(fileTree("dir" to "${lb.get().outputs.dir("lib")}", "include" to "**/*.jar"))
    implementation(files( { lb.get().extra["lblibs"] } ))

    // tests
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation(fileTree("$rootDir/lib-test"))
//    testImplementation(testFixtures(project(":lphybeast")))
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

val developers = "LPhyBEAST developer team"
tasks.jar {
    manifest {
        // shared attr in the root build
        attributes(
            "Implementation-Title" to "LPhyBEAST sampled ancestor trees",
            "Implementation-Vendor" to developers,
        )
    }
}

tasks.test {
    useJUnitPlatform()
    // set heap size for the test JVM(s)
    minHeapSize = "128m"
    maxHeapSize = "1G"
    // show standard out and standard error of the test JVM(s) on the console
    testLogging.showStandardStreams = true

    reports {
        junitXml.apply {
            isOutputPerTestCase = true // defaults to false
            mergeReruns.set(true) // defaults to false
        }
    }
}

