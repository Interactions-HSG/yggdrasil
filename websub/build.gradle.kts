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

  implementation(libs.guava)

  testImplementation(libs.junit)
  testImplementation(libs.vertx.unit)
}
