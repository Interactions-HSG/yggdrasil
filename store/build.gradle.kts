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

  implementation("org.eclipse.rdf4j:rdf4j-model:4.2.3")
  implementation("org.eclipse.rdf4j:rdf4j-repository-sail:4.2.3")
  implementation("org.eclipse.rdf4j:rdf4j-sail-memory:4.2.3")
  implementation("org.eclipse.rdf4j:rdf4j-sail-nativerdf:4.2.3")

  implementation("org.apache.commons:commons-rdf-api:0.5.0")
  implementation("org.apache.commons:commons-rdf-rdf4j:0.5.0")

  testImplementation("junit:junit:4.13.2")
  testImplementation("io.vertx:vertx-unit:$vertxVersion")
}
