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
  implementation(libs.vertx.core)

  // currently using full rdf4j lib
  implementation(libs.rdf4j.storage)

  implementation(libs.gson)
  implementation(libs.wot.td.java)

  implementation ("org.apache.logging.log4j:log4j-api:2.23.1")
  implementation ("org.apache.logging.log4j:log4j-core:2.23.1")

  implementation(libs.hmas.java)
//  implementation(files("../libs/HMAS/bindings-1.0-SNAPSHOT.jar"))
//  implementation(files("../libs/HMAS/core-1.0-SNAPSHOT.jar"))
//  implementation(files("../libs/HMAS/interaction-1.0-SNAPSHOT.jar"))
//  implementation("com.google.guava:guava:11.0.2")
//  implementation("io.vavr:vavr:0.10.4")
//  implementation("org.apache.httpcomponents.client5:httpclient5:5.2.2")
//  implementation("org.apache.httpcomponents.core5:httpcore5:5.2.2")


  compileOnly(libs.spotbugs.annotations)
  pmd(libs.pmd.java)
  pmd(libs.pmd.ant)

  testImplementation(platform(libs.junit.platform))
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.vertx.junit5)

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
