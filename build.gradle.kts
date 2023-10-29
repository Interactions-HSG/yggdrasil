import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  application
  eclipse
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
  implementation("io.vertx:vertx-core:$vertxVersion")
  implementation("io.vertx:vertx-config:$vertxVersion")
  implementation("io.vertx:vertx-web:$vertxVersion")
  implementation("io.vertx:vertx-web-api-contract:$vertxVersion")
  implementation("io.vertx:vertx-web-client:$vertxVersion")
  implementation("io.github.classgraph:classgraph:4.8.157")

  implementation("org.apache.httpcomponents.client5:httpclient5:5.2.1")
  implementation("org.apache.httpcomponents.client5:httpclient5-fluent:5.2.1")

  implementation(files("libs/cartago-2.5.jar"))
  implementation("com.github.Interactions-HSG:wot-td-java:v0.1.1")

  implementation("org.eclipse.rdf4j:rdf4j-model:4.2.3")
  implementation("org.eclipse.rdf4j:rdf4j-repository-sail:4.2.3")
  implementation("org.eclipse.rdf4j:rdf4j-sail-memory:4.2.3")
  implementation("org.eclipse.rdf4j:rdf4j-sail-nativerdf:4.2.3")

  implementation("org.apache.commons:commons-rdf-api:0.5.0")
  implementation("org.apache.commons:commons-rdf-rdf4j:0.5.0")
  implementation("com.google.code.gson:gson:2.10.1")

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
