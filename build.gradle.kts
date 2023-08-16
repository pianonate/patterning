val patterningMain: String = "patterning.Patterning"
val pathToJoglLibraries = "/Applications/Processing.app/Contents/Java/core/library/"
val platforms =
    listOf("macos-aarch64", "macos-x86_64", "windows-amd64", "linux-amd64", "linux-arm", "linux-aarch64")

group = "org.patterning"
version = "1.0-SNAPSHOT"

plugins {
    application
    kotlin("jvm") version "1.9.0"
    
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
    mavenLocal()
}

dependencies {
    implementation("com.processing:processing:4.3")
    implementation("org.jogamp.gluegen:gluegen-rt:2.4.0")
    implementation("org.jogamp.jogl:jogl-all:2.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation(kotlin("stdlib-jdk8"))
}

application {
    mainClass.set(patterningMain)
    
    applicationDefaultJvmArgs = listOf(
        "-Djava.library.path=${platforms.joinToString(separator = ":") { "$pathToJoglLibraries$it" }}",
        "-Xmx16G",
        "-XX:ParallelGCThreads=8",
        // used to debug native library loading
        // "-Djogamp.debug.NativeLibrary=true"
    )
}


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}


tasks.register<JavaExec>("profile") {
    sourceSets["main"].runtimeClasspath.also { this.classpath = it }
    jvmArgs("-agentpath:/Applications/YourKit-Java-Profiler-2022.9.app/Contents/Resources/bin/mac/libyjpagent.dylib=disablestacktelemetry,exceptions=disable,delay=10000")
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    manifest {
        attributes["Main-Class"] = patterningMain
    }
    
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}