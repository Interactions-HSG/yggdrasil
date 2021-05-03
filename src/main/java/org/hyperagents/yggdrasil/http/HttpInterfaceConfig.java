package org.hyperagents.yggdrasil.http;

import io.vertx.core.json.JsonObject;

import java.util.Optional;

public class HttpInterfaceConfig {
  private String host = "0.0.0.0";
  private int port = 8080;

  private String webSubHubIRI;

  public HttpInterfaceConfig(JsonObject config) {
    JsonObject httpConfig = config.getJsonObject("http-config");

    if (httpConfig != null) {
      host = httpConfig.getString("host", "0.0.0.0");
      port = httpConfig.getInteger("port", 8080);

      webSubHubIRI = httpConfig.getString("websub-hub");
    }
  }

  public String getHost() {
    return this.host;
  }

  public int getPort() {
    return this.port;
  }

  public Optional<String> getWebSubHubIRI() {
    return (webSubHubIRI == null) ? Optional.empty() : Optional.of(webSubHubIRI);
  }
}
