
rootProject.name = "LPhyBEAST"

// must follow this folder structure
var lphyPath = "../linguaPhylo"
includeBuild(lphyPath)
// bad change of Gradle 8 to deprecate including sub-projects from outside the root project
// watching https://github.com/gradle/gradle/issues/18644
var lphyNameStr = "lphy"
//includeBuild("$lphyPath/lphyNameStr")
include(lphyNameStr)
project(":${lphyNameStr}").projectDir = file("${lphyPath}/${lphyNameStr}")

include("lphybeast")

// https://docs.gradle.org/current/userguide/build_cache.html
// https://docs.gradle.org/current/userguide/build_cache_use_cases.html
buildCache {
    local {
        directory = File(rootDir, "build-cache")
        removeUnusedEntriesAfterDays = 30
        println("Creating local build cache : ${directory}")
    }
}
