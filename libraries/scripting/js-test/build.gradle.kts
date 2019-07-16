
plugins {
    kotlin("jvm")
}

jvmTarget = "1.6"

val embeddableTestRuntime by configurations.creating

dependencies {
    testCompile(commonDep("junit"))

    testCompile(project(":kotlin-script-runtime"))
    testCompile(project(":kotlin-scripting-js"))
    testCompile(kotlinStdlib())
    testCompile(project(":kotlin-scripting-common"))
    testCompile(project(":kotlin-scripting-compiler"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:backend.js"))
    testCompile(project(":js:js.engines"))
    testRuntime(project(":kotlin-reflect"))
    testRuntimeOnly(project(":kotlin-reflect-api"))
    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntimeOnly(intellijDep()) { includeJars("openapi", "idea", "idea_rt", "log4j", "picocontainer-1.2", "guava-25.1-jre", "jdom") }
    testRuntimeOnly(commonDep("org.jetbrains.intellij.deps", "trove4j"))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    workingDir = rootDir
}
