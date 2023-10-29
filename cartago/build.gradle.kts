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

  implementation("io.vertx:vertx-core:$vertxVersion")

  implementation("org.apache.httpcomponents:httpcore:4.4.16")

  implementation(files("libs/cartago-2.5.jar"))
  implementation("com.github.Interactions-HSG:wot-td-java:v0.1.1")

  implementation("org.eclipse.rdf4j:rdf4j-model:4.2.3")

  implementation("org.apache.commons:commons-rdf-api:0.5.0")
  implementation("org.apache.commons:commons-rdf-rdf4j:0.5.0")

  testImplementation("junit:junit:4.13.2")
  testImplementation("io.vertx:vertx-unit:$vertxVersion")

  testImplementation("org.apache.httpcomponents.client5:httpclient5:5.2.1")
  testImplementation("org.apache.httpcomponents.client5:httpclient5-fluent:5.2.1")
}
