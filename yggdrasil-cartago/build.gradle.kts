plugins {
  java
  `java-library`
  checkstyle
  pmd
  alias(libs.plugins.spotbugs)
  jacoco
}

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

  implementation(libs.log4j.core)
  implementation(libs.vertx.core)

  implementation(libs.httpcomponents.core5)

  implementation(files("${rootProject.projectDir}/libs/cartago-3.2-SNAPSHOT-all.jar"))
  implementation(files("${rootProject.projectDir}/libs/hmas/hmas-core-1.0-SNAPSHOT-all.jar"))
  implementation(files("${rootProject.projectDir}/libs/hmas/hmas-interaction-1.0-SNAPSHOT-all.jar"))
  implementation(files("${rootProject.projectDir}/libs/hmas/hmas-bindings-all.jar"))

  implementation(libs.wot.td.java)
  // implementation(libs.hmas.java)

  implementation(libs.rdf4j.model)
  implementation(libs.rdf4j.runtime)

  implementation(libs.gson)

  implementation(libs.apache.commons.lang3)

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

tasks {
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
