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

  implementation(libs.httpcomponents.core)

  implementation(files("libs/cartago-2.5.jar"))
  implementation(libs.wot.td.java)

  implementation(libs.rdf4j.model)

  implementation(libs.commons.rdf.api)
  implementation(libs.commons.rdf.rdf4j)

  testImplementation(libs.junit)
  testImplementation(libs.vertx.unit)

  testImplementation(libs.httpcomponents.httpclient5)
  testImplementation(libs.httpcomponents.httpclient5.fluent)
}
