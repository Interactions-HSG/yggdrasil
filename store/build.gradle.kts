plugins {
  java
  `java-library`
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
  implementation(project(":utils"))
  implementation(project(":messages"))

  implementation(libs.vertx.core)
  implementation(libs.vertx.web.client)

  implementation(libs.httpcomponents.core)

  implementation(libs.commons.lang3)

  implementation(libs.rdf4j.model)
  implementation(libs.rdf4j.repository.sail)
  implementation(libs.rdf4j.sail.memory)
  implementation(libs.rdf4j.sail.nativerdf)

  implementation(libs.commons.rdf.api)
  implementation(libs.commons.rdf.rdf4j)

  testImplementation(libs.junit)
  testImplementation(libs.vertx.unit)
}
