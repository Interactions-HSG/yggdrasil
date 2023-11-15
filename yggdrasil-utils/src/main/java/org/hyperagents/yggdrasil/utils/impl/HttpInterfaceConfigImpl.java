package org.hyperagents.yggdrasil.utils.impl;

import io.vertx.core.json.JsonObject;
import java.util.Optional;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public class HttpInterfaceConfigImpl implements HttpInterfaceConfig {
  private final String host;
  private final String baseUri;
  private final int port;
  private final Optional<String> webSubHubUri;

  public HttpInterfaceConfigImpl(final JsonObject config) {
    final var httpConfig = Optional.ofNullable(config.getJsonObject("http-config"));
    this.host = httpConfig.map(c -> c.getString("host", "0.0.0.0")).orElse("0.0.0.0");
    this.port = httpConfig.map(c -> c.getInteger("port", 8080)).orElse(8080);
    this.webSubHubUri = httpConfig.map(c -> c.getString("websub-hub-uri"));
    this.baseUri =
      httpConfig
        .map(c -> c.getString("base-uri"))
        // Strip away the trailing slash (if any)
        .map(u -> u.endsWith("/") ? u.substring(0, u.length() - 1) : u)
        .orElseGet(() -> "http://"
                         + (this.host.equals("0.0.0.0") ? "localhost" : this.host)
                         + ":"
                         + this.port);
  }

  @Override
  public String getHost() {
    return this.host;
  }

  @Override
  public int getPort() {
    return this.port;
  }

  @Override
  public String getBaseUri() {
    return this.baseUri;
  }

  @Override
  public Optional<String> getWebSubHubUri() {
    return this.webSubHubUri;
  }
}