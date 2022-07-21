plugins {
    `java-library`
    `java-test-fixtures` // which produces test fixtures
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
tasks.jar.get().enabled = false
tasks.testFixturesJar.get().enabled = false

version = "0.1.0-SNAPSHOT"
base.archivesName.set("lphybeastExt")

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
