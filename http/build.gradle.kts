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
  implementation(project(":utils"))
  implementation(project(":messages"))
  implementation(project(":cartago"))
  implementation(project(":websub"))

  implementation(libs.vertx.core)
  implementation(libs.vertx.web)

  implementation(libs.wot.td.java)

  implementation(libs.httpcomponents.core)

  implementation(libs.guava)

  implementation(libs.gson)

  implementation(libs.commons.rdf.api)
  implementation(libs.commons.rdf.rdf4j)

  compileOnly(libs.spotbugs.annotations)
  pmd(libs.pmd.java)
  pmd(libs.pmd.ant)

  testImplementation(libs.junit)
  testImplementation(libs.vertx.unit)

  testImplementation(project(":store"))

  testImplementation(libs.httpcomponents.httpclient5)
  testImplementation(libs.httpcomponents.httpclient5.fluent)

  testImplementation(libs.vertx.web.client)

  testImplementation(libs.rdf4j.model)
  testImplementation(libs.rdf4j.repository.sail)
  testImplementation(libs.rdf4j.sail.memory)
  testImplementation(libs.rdf4j.sail.nativerdf)

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
