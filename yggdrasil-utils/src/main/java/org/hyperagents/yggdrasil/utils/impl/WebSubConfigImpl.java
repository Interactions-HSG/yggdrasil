package org.hyperagents.yggdrasil.utils.impl;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.JsonObjectUtils;
import org.hyperagents.yggdrasil.utils.WebSubConfig;

/**
 * Implementation of the WebSubConfig interface.
 * This class represents the configuration for WebSub,
 * a protocol for distributed publish-subscribe communication on the web.
 * It provides methods to retrieve the configuration settings for WebSub,
 * such as whether it is enabled and the WebSub hub URI.
 */
public class WebSubConfigImpl implements WebSubConfig {
  private static final Logger LOGGER = LogManager.getLogger(WebSubConfigImpl.class);

  private final boolean enabled;
  private final String webSubHubUri;

  /**
   * Constructs a new WebSubConfigImpl object with the specified configuration
   * and HTTP interface configuration.
   *
   * @param config The JSON object containing the WebSub configuration settings.
   * @param httpConfig The HTTP interface configuration.
   */
  public WebSubConfigImpl(final JsonObject config, final HttpInterfaceConfig httpConfig) {
    final var webSubConfig =
        JsonObjectUtils.getJsonObject(config, "notification-config", LOGGER::error);
    this.enabled =
      webSubConfig.flatMap(c -> JsonObjectUtils.getBoolean(c, "enabled", LOGGER::error))
                  .orElse(false);
    this.webSubHubUri =
      webSubConfig.flatMap(c -> JsonObjectUtils.getString(c, "websub-hub-base-uri", LOGGER::error))
                  .orElse(httpConfig.getBaseUriTrailingSlash())
      + "hub/";
  }

  @Override
  public boolean isEnabled() {
    return this.enabled;
  }

  @Override
  public String getWebSubHubUri() {
    return this.webSubHubUri;
  }
}
