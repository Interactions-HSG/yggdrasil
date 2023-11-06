plugins {
  java
  `java-library`
  checkstyle
  pmd
  alias(libs.plugins.spotbugs)
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

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
  implementation(project(":yggdrasil-utils"))
  implementation(project(":yggdrasil-cartago"))
  implementation(project(":yggdrasil-websub"))

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

  testImplementation(libs.junit)
  testImplementation(libs.vertx.unit)

  testImplementation(libs.httpcomponents.httpclient5)
  testImplementation(libs.httpcomponents.httpclient5.fluent)

  testImplementation(libs.vertx.web.client)

  testCompileOnly(libs.spotbugs.annotations)
}

tasks {
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
