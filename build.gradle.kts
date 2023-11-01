import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  application
  java
  alias(libs.plugins.shadowJar)
  checkstyle
  pmd
  alias(libs.plugins.spotbugs)
}

defaultTasks = mutableListOf("shadowJar")

checkstyle {
  config = resources.text.fromFile("${rootProject.projectDir}/checkstyle.xml")
  toolVersion = libs.versions.checkstyle.get()
}

pmd {
  toolVersion = libs.versions.pmd.get()
  ruleSetConfig = resources.text.fromFile("${rootProject.projectDir}/pmd.xml")
}

spotbugs {
  toolVersion = libs.versions.spotbugs
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

allprojects {

  repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }

  group = "org.hyperagents"
  version = "0.0-SNAPSHOT"
}

dependencies {
  implementation(project(":http"))
  implementation(project(":cartago"))
  implementation(project(":store"))
  implementation(project(":websub"))

  implementation(libs.vertx.core)
  implementation(libs.vertx.config)

  compileOnly(libs.spotbugs.annotations)
  pmd(libs.pmd.java)
  pmd(libs.pmd.ant)

  testImplementation(libs.junit)
  testImplementation(libs.vertx.unit)

  testCompileOnly(libs.spotbugs.annotations)
}

application {
  mainClass = "io.vertx.core.Launcher"
}

val mainVerticleName = "org.hyperagents.yggdrasil.MainVerticle"
val watchForChange = "src/**/*"
val doOnChange = "./gradlew classes"

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
    options.compilerArgs.addAll(listOf("-parameters"))
  }

  spotbugsMain {
    reports.create("html") {
        required.set(true)
    }
  }
  spotbugsTest {
    reports.create("html") {
        required.set(true)
    }
  }
}
