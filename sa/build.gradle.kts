plugins {
    `java-library`
//    `java-test-fixtures` // which produces test fixtures
}

version = "0.2.0"
base.archivesName.set("sa-lb")

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

