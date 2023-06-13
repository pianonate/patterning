import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val patterningMain: String = "patterning.Patterning"

group = "com.example"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_19
    targetCompatibility = JavaVersion.VERSION_19
}


tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "19"
    }
}

plugins {
    application
    kotlin("jvm") version "1.8.21"
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
    mavenLocal()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    implementation("com.processing:processing:4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set(patterningMain)
    applicationDefaultJvmArgs = listOf("-XX:+UseZGC")
}

tasks.withType<JavaCompile>().configureEach {
    options.isDeprecation = true
    options.compilerArgs = listOf("-Xlint:unchecked")
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