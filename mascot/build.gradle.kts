plugins {
    `java-library`
//    `java-test-fixtures` // which produces test fixtures
}

version = "0.2.1"
base.archivesName.set("mascot-lb")

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

dependencies {
    implementation(project(":lphybeast-ext"))

    implementation(fileTree("lib"))

    // tests
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation(project(":lphybeast-ext"))
//    testImplementation(testFixtures(project(":lphybeast-ext")))

//    testImplementation(fileTree("$rootDir/lib-test"))
//    testImplementation(testFixtures("io.github.linguaphylo:lphybeast:0.4.0-SNAPSHOT"))
//    testFixturesImplementation("io.github.linguaphylo:lphybeast:0.4.0-SNAPSHOT")
}
//tasks.compileJava.get().dependsOn("installLPhyBEAST")

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
tasks.register("runMascotLB", JavaExec::class.java) {
    // use classpath
    jvmArgs = listOf("-cp", sourceSets.main.get().runtimeClasspath.asPath)
    println("clspath = ${sourceSets.main.get().runtimeClasspath.asPath}")
    mainClass.set("lphybeast.LPhyBEAST")
    setArgs(listOf("-o", "$rootDir/../linguaPhylo/tmp/h3n2.xml",
            "$rootDir/../linguaPhylo/tutorials/h3n2.lphy"))
    println("args = $args")
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
