plugins {
  java
  `java-library`
}

repositories {
  mavenCentral()
}

group = "org.hyperagents"
version = "0.0-SNAPSHOT"

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

val vertxVersion = "3.9.7"

dependencies {
  implementation(project(":utils"))
  implementation(project(":messages"))

  implementation("io.vertx:vertx-core:$vertxVersion")
  implementation("io.vertx:vertx-web-client:$vertxVersion")

  implementation("org.apache.httpcomponents:httpcore:4.4.16")

  implementation("com.google.guava:guava:31.1-jre")

  testImplementation("junit:junit:4.13.2")
  testImplementation("io.vertx:vertx-unit:$vertxVersion")
}
