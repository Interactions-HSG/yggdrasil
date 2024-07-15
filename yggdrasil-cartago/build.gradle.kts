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

  // implementation(files("${rootProject.projectDir}/libs/cartago-3.1.jar"))
  implementation(files("${rootProject.projectDir}/libs/cartago-3.2-SNAPSHOT-all.jar"))
  implementation(libs.wot.td.java)

  implementation(libs.rdf4j.model)
  implementation(libs.rdf4j.runtime)


  implementation(files("../libs/HMAS/bindings-1.0-SNAPSHOT.jar"))
  implementation(files("../libs/HMAS/core-1.0-SNAPSHOT.jar"))
  implementation(files("../libs/HMAS/interaction-1.0-SNAPSHOT.jar"))
  implementation("com.google.guava:guava:33.2.1-jre")
  implementation("io.vavr:vavr:0.10.4")
  implementation("com.google.code.gson:gson:2.10.1")
  implementation("org.apache.httpcomponents.client5:httpclient5:5.2.2")
  implementation("org.apache.httpcomponents.core5:httpcore5:5.2.2")


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
