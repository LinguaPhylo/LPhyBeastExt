
rootProject.name = "LPhyBEAST"

// Comment out 3 lines below, if use project(":lphy") in dependencies
// must follow this folder structure, and it will import IntelliJ module as well
//includeBuild("../linguaPhylo")
//include("lphy")
// Gradle 8 to deprecate including sub-projects from outside the root project
// watching https://github.com/gradle/gradle/issues/18644
//project(":lphy").projectDir = file("../linguaPhylo/lphy")

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
