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

repositories {
  maven {
      url = uri("https://raw.github.com/jacamo-lang/mvn-repo/master")
  }
  maven {
      url = uri("https://repo.gradle.org/gradle/libs-releases/")  // For current version of Gradle tooling API
  }
  maven {
      url = uri("https://repo.gradle.org/gradle/libs-releases-local/") // For older versions of Gradle tooling API
  }
}

dependencies {
  implementation(project(":yggdrasil-utils"))
  implementation(project(":yggdrasil-websub"))

  implementation(libs.log4j.core)
  implementation(libs.vertx.core)

  implementation(libs.httpcomponents.core)

  implementation(files("libs/cartago-3.1.jar"))
  implementation(libs.jacamo)
  implementation(libs.wot.td.java)

  implementation(libs.apache.commons.lang3)

  implementation(libs.rdf4j.model)

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

tasks {
  test {
    useJUnitPlatform()
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