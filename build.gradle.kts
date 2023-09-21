/**
 * change this for _your_ configuration
 */
val pathToCore = "/Applications/Processing.app/Contents/Java/"

/**
 * leave the rest of this alone unless you know what you're doing
 * if you know anything about gradle, you probably know more than me so if you feel confident
 * have at it
 */
val pathToJoglLibraries = "${pathToCore}core/library/"
val patterningMain: String = "patterning.Patterning"
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
}

dependencies {
    implementation(files("${pathToCore}core.jar"))
    implementation(files("${pathToJoglLibraries}gluegen-rt.jar"))
    implementation(files("${pathToJoglLibraries}jogl-all.jar"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test"))
}

application {
    mainClass.set(patterningMain)

    applicationDefaultJvmArgs = listOf(
        "-Djava.library.path=${platforms.joinToString(separator = ":") { "$pathToJoglLibraries$it" }}",
        "-Xmx16G",
        //"-XX:ParallelGCThreads=8",
        "-XX:+UseZGC"
        // used to debug native library loading
        // "-Djogamp.debug.NativeLibrary=true"
    )
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(20))
    }
}

sourceSets {
    getByName("main") {
        kotlin.srcDirs("src/main/kotlin")
        resources.srcDirs("src/main/resources")
    }
    getByName("test") {
        kotlin.srcDirs("src/test/kotlin")
    }
}

tasks.register<JavaExec>("profile") {
    sourceSets["main"].runtimeClasspath.also { this.classpath = it }
    jvmArgs("-agentpath:/Applications/YourKit-Java-Profiler-2022.9.app/Contents/Resources/bin/mac/libyjpagent.dylib=disablestacktelemetry,exceptions=disable,delay=10000")
}

/*tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = patterningMain
    }

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}*/

tasks.named<Copy>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.WARN
    from("src/main/resources") {
        include("**/*.rle")
        include("**/*.png")
        // Add your includes here
    }
}

tasks.test {
    useJUnitPlatform()
}
