import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  application
  java
  alias(libs.plugins.shadowJar)
  checkstyle
  pmd
  alias(libs.plugins.spotbugs)
  jacoco
  id("jacoco-report-aggregation")
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

allprojects {

  repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }

  group = "org.hyperagents"
  version = "0.0.0-SNAPSHOT"
}

dependencies {
  implementation(project(":yggdrasil-utils"))
  implementation(project(":yggdrasil-core"))
  implementation(project(":yggdrasil-cartago"))
  implementation(project(":yggdrasil-websub"))


  implementation(files("libs/HMAS/bindings-1.0-SNAPSHOT.jar"))
  implementation(files("libs/HMAS/core-1.0-SNAPSHOT.jar"))
  implementation(files("libs/HMAS/interaction-1.0-SNAPSHOT.jar"))
  implementation("org.eclipse.rdf4j:rdf4j-runtime:3.7.4@pom") {
    isTransitive = true
  }
  implementation("com.google.guava:guava:11.0.2")
  implementation("io.vavr:vavr:0.10.4")
  implementation("com.google.code.gson:gson:2.10.1")
  implementation("org.apache.httpcomponents.client5:httpclient5:5.2.2")
  implementation("org.apache.httpcomponents.core5:httpcore5:5.2.2")

  implementation(libs.vertx.core)
  implementation(libs.vertx.config)

  compileOnly(libs.spotbugs.annotations)
  pmd(libs.pmd.java)
  pmd(libs.pmd.ant)

  testImplementation(platform(libs.junit.platform))
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.vertx.junit5)
  testImplementation(libs.vertx.web)
  testImplementation(libs.vertx.web.client)
  testImplementation(libs.httpcomponents.core5)
  testImplementation(libs.wot.td.java)
  testImplementation(libs.rdf4j.model)
  testImplementation(files("libs/cartago-3.1.jar"))

  testCompileOnly(libs.spotbugs.annotations)
}

application {
  mainClass = "io.vertx.core.Launcher"
}

val mainVerticleName = "org.hyperagents.yggdrasil.MainVerticle"

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
    args = mutableListOf("run", mainVerticleName, "--launcher-class=${application.mainClass.get()}")
  }

  compileJava {
    options.compilerArgs.addAll(listOf("-parameters"))
  }

  test {
    useJUnitPlatform()
    finalizedBy(jacocoTestReport)
  }

  check {
    dependsOn(named<JacocoReport>("testCodeCoverageReport"))
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
