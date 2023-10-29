plugins {
  java
  `java-library`
}

repositories {
  mavenCentral()
  maven { url = uri("https://jitpack.io") }
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
  implementation(project(":cartago"))
  implementation(project(":websub"))

  implementation("io.vertx:vertx-core:$vertxVersion")
  implementation("io.vertx:vertx-web:$vertxVersion")

  implementation("com.github.Interactions-HSG:wot-td-java:v0.1.1")

  implementation("org.apache.httpcomponents:httpcore:4.4.16")

  implementation("com.google.guava:guava:31.1-jre")

  implementation("com.google.code.gson:gson:2.10.1")

  implementation("org.apache.commons:commons-rdf-api:0.5.0")
  implementation("org.apache.commons:commons-rdf-rdf4j:0.5.0")

  testImplementation("junit:junit:4.13.2")
  testImplementation("io.vertx:vertx-unit:$vertxVersion")

  testImplementation(project(":store"))

  testImplementation("org.apache.httpcomponents.client5:httpclient5:5.2.1")
  testImplementation("org.apache.httpcomponents.client5:httpclient5-fluent:5.2.1")

  testImplementation("io.vertx:vertx-web-client:$vertxVersion")

  testImplementation("org.eclipse.rdf4j:rdf4j-model:4.2.3")
  testImplementation("org.eclipse.rdf4j:rdf4j-repository-sail:4.2.3")
  testImplementation("org.eclipse.rdf4j:rdf4j-sail-memory:4.2.3")
  testImplementation("org.eclipse.rdf4j:rdf4j-sail-nativerdf:4.2.3")
}
