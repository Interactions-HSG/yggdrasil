package org.hyperagents.yggdrasil.http;

import io.vertx.core.json.JsonObject;

import java.util.Optional;

public class HttpInterfaceConfig {
  private String host = "0.0.0.0";
  private String baseUri = null;
  private int port = 8080;

  private String webSubHubUri;

  public HttpInterfaceConfig(JsonObject config) {
    JsonObject httpConfig = config.getJsonObject("http-config");

    if (httpConfig != null) {
      host = httpConfig.getString("host", "0.0.0.0");
      port = httpConfig.getInteger("port", 8080);
      webSubHubUri = httpConfig.getString("websub-hub-uri");

      baseUri = httpConfig.getString("base-uri");
      // Strip away the trailing slash (if any)
      if (baseUri != null && baseUri.endsWith("/")) {
        baseUri = baseUri.substring(0, baseUri.length()-1);
      }
    }
  }

  public String getHost() {
    return this.host;
  }

  public int getPort() {
    return this.port;
  }

  public String getBaseUri() {
    if (baseUri == null) {
      String hostname = host.equals("0.0.0.0") ? "localhost" : host;
      return "http://" + hostname + ":" + port;
    }

    return baseUri;
  }

  public Optional<String> getWebSubHubUri() {
    return (webSubHubUri == null) ? Optional.empty() : Optional.of(webSubHubUri);
  }
}
