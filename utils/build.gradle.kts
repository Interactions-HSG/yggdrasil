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
  implementation("io.vertx:vertx-core:$vertxVersion")

  testImplementation("junit:junit:4.13.2")
  testImplementation("io.vertx:vertx-unit:$vertxVersion")
}
