import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  java
  application
  alias(libs.plugins.shadowJar)
  checkstyle
  pmd
  alias(libs.plugins.spotbugs)
  jacoco
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

jacoco {
  toolVersion = libs.versions.jacoco.get()
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
  implementation(project(":yggdrasil-utils"))
  implementation(project(":yggdrasil-cartago"))
  implementation(project(":yggdrasil-websub"))

  implementation(libs.log4j.core)
  implementation(libs.vertx.core)
  implementation(libs.vertx.web)
  implementation(libs.vertx.web.client)

  implementation(libs.wot.td.java)

  implementation(libs.httpcomponents.core)

  implementation(libs.guava)
  implementation(libs.gson)

  implementation(libs.rdf4j.model)
  implementation(libs.rdf4j.repository.sail)
  implementation(libs.rdf4j.sail.memory)
  implementation(libs.rdf4j.sail.nativerdf)

  implementation(libs.commons.rdf.api)
  implementation(libs.commons.rdf.rdf4j)

  compileOnly(libs.spotbugs.annotations)
  pmd(libs.pmd.java)
  pmd(libs.pmd.ant)

  testImplementation(platform(libs.junit.platform))
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.vertx.junit5)

  testImplementation(libs.httpcomponents.httpclient5)
  testImplementation(libs.httpcomponents.httpclient5.fluent)

  testCompileOnly(libs.spotbugs.annotations)
}

application {
  mainClass = "io.vertx.core.Launcher"
}

val mainVerticleName = "org.hyperagents.yggdrasil.DefaultMainVerticle"

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
    args = mutableListOf("run", mainVerticleName, "--launcher-class=${application.mainClass}")
  }

  compileJava {
    options.compilerArgs.addAll(listOf("-parameters"))
  }

  test {
    useJUnitPlatform()
    finalizedBy(jacocoTestReport)
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
