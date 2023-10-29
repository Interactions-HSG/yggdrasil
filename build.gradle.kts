import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  application
  java
  id("com.github.johnrengelman.shadow") version "8.1.1"
}

defaultTasks = mutableListOf("shadowJar")

repositories {
  mavenCentral()
  maven { url = uri("https://jitpack.io") }
}

group = "org.hyperagents"
version = "0.0-SNAPSHOT"

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

application {
  mainClass = "io.vertx.core.Launcher"
}

val vertxVersion = "3.9.7"
val mainVerticleName = "org.hyperagents.yggdrasil.MainVerticle"
val watchForChange = "src/**/*"
val doOnChange = "./gradlew classes"

dependencies {
  implementation(project(":http"))
  implementation(project(":cartago"))
  implementation(project(":store"))
  implementation(project(":websub"))

  implementation("io.vertx:vertx-core:$vertxVersion")
  implementation("io.vertx:vertx-config:$vertxVersion")

  testImplementation("junit:junit:4.13.2")
  testImplementation("io.vertx:vertx-unit:$vertxVersion")
}

tasks {
  named<ShadowJar>("shadowJar") {
    manifest {
      attributes(mapOf("Main-Verticle" to mainVerticleName))
    }
    mergeServiceFiles {
      include("META-INF/services/io.vertx.core.spi.VerticleFactory")
    }
  }

  named<JavaExec>("run") {
    args = mutableListOf("run", mainVerticleName, "--redeploy=$watchForChange", "--launcher-class=${application.mainClass}", "--on-redeploy=$doOnChange")
  }

  compileJava {
    options.compilerArgs.add("-parameters")
  }
}
