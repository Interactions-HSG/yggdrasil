plugins {
  java
  `java-library`
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
  implementation(libs.vertx.core)

  implementation(libs.gson)

  testImplementation(libs.junit)
  testImplementation(libs.vertx.unit)
}
