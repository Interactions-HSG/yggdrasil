package org.hyperagents.yggdrasil.utils.impl;

import io.vertx.core.json.JsonObject;
import org.apache.log4j.Logger;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.JsonObjectUtils;

public class EnvironmentConfigImpl implements EnvironmentConfig {
  private static final Logger LOGGER = Logger.getLogger(EnvironmentConfigImpl.class);

  private final boolean enabled;

  public EnvironmentConfigImpl(final JsonObject config) {
    final var environmentConfig =
        JsonObjectUtils.getJsonObject(config, "notification-config", LOGGER::error);
    this.enabled =
      environmentConfig.flatMap(c -> JsonObjectUtils.getBoolean(c, "enabled", LOGGER::error))
                       .orElse(false);
  }

  @Override
  public boolean isEnabled() {
    return this.enabled;
  }
}
