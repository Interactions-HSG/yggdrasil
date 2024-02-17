package org.hyperagents.yggdrasil.utils.impl;

import io.vertx.core.json.JsonObject;
import org.apache.log4j.Logger;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.JsonObjectUtils;
import org.hyperagents.yggdrasil.utils.WebSubConfig;

/**
 * TODO: Javadoc.
 */
public class WebSubConfigImpl implements WebSubConfig {
  private static final Logger LOGGER = Logger.getLogger(WebSubConfigImpl.class);

  private final boolean enabled;
  private final String webSubHubUri;

  /**
   * TODO: Javadoc.
   */
  public WebSubConfigImpl(final JsonObject config, final HttpInterfaceConfig httpConfig) {
    final var webSubConfig =
        JsonObjectUtils.getJsonObject(config, "notification-config", LOGGER::error);
    this.enabled =
      webSubConfig.flatMap(c -> JsonObjectUtils.getBoolean(c, "enabled", LOGGER::error))
                  .orElse(false);
    this.webSubHubUri =
      webSubConfig.flatMap(c -> JsonObjectUtils.getString(c, "websub-hub-base-uri", LOGGER::error))
                  .orElse(httpConfig.getBaseUri())
      + "/hub/";
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
