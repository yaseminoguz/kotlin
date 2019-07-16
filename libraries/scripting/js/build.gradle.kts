
plugins {
    kotlin("jvm")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-script-runtime"))
    compile(kotlinStdlib())
    compile(project(":kotlin-scripting-common"))
    compileOnly(project(":kotlin-scripting-compiler"))
    compile(project(":kotlin-scripting-compiler-impl"))
    compile(project(":compiler:cli"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:backend-common"))
    compile(project(":compiler:ir.tree"))
    compile(project(":compiler:ir.psi2ir"))
    compile(project(":compiler:ir.backend.common"))
    compile(project(":compiler:ir.serialization.common"))
    compile(project(":compiler:ir.serialization.js"))
    compile(project(":js:js.ast"))
    compile(project(":js:js.frontend"))
    compile(project(":compiler:backend.js"))
    compile(project(":js:js.engines"))
    compile(project(":kotlin-reflect-api"))
    compile(intellijCoreDep()) { includeJars("intellij-core") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {  }
}

publish()

standardPublicJars()
