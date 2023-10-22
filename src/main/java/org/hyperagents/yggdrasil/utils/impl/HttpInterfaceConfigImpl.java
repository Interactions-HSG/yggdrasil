package org.hyperagents.yggdrasil.utils;

import io.vertx.core.json.JsonObject;

import java.util.Optional;

public class HttpInterfaceConfigImpl {
  private final String host;
  private String baseUri = null;
  private final int port;
  private final Optional<String> webSubHubUri;

  public HttpInterfaceConfig(final JsonObject config) {
    final var httpConfig = Optional.ofNullable(config.getJsonObject("http-config"));
    this.host = httpConfig.map(c -> c.getString("host", "0.0.0.0")).orElse("0.0.0.0");
    this.port = httpConfig.map(c -> c.getInteger("port", 8080)).orElse(8080);
    this.webSubHubUri = httpConfig.map(c -> c.getString("websub-hub-uri"));
    this.baseUri =
      httpConfig
        .map(c -> c.getString("base-uri"))
        .map(u -> {
          // Strip away the trailing slash (if any)
          if (u.endsWith("/")) {
            return u.substring(0, u.length() - 1);
          } else {
            return u;
          }
        })
        .orElseGet(() -> "http://" + (this.host.equals("0.0.0.0") ? "localhost" : this.host) + ":" + this.port);
  }

  public String getHost() {
    return this.host;
  }

  public int getPort() {
    return this.port;
  }

  public String getBaseUri() {
    return this.baseUri;
  }

  public Optional<String> getWebSubHubUri() {
    return this.webSubHubUri;
  }
}
