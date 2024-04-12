package org.hyperagents.yggdrasil.utils.impl;

import io.vertx.core.json.JsonObject;
import org.apache.log4j.Logger;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.JsonObjectUtils;

import java.util.Optional;

/**
 * Represents an implementation of the EnvironmentConfig interface.
 * This class provides functionality to retrieve and manage environment configuration settings.
 */
public class EnvironmentConfigImpl implements EnvironmentConfig {
  private static final Logger LOGGER = Logger.getLogger(EnvironmentConfigImpl.class);

  private final boolean enabled;

  private final Optional<JsonObject> environmentConfig;

  /**
   * Constructs a new EnvironmentConfigImpl object with the specified configuration.
   *
   * @param config The JSON object containing the environment configuration settings.
   */
  public EnvironmentConfigImpl(final JsonObject config) {
    environmentConfig =
        JsonObjectUtils.getJsonObject(config, "environment-config", LOGGER::error);
    this.enabled =
      environmentConfig.flatMap(c -> JsonObjectUtils.getBoolean(c, "enabled", LOGGER::error))
                       .orElse(false);
  }

  /**
   * Checks if the environment configuration is enabled.
   *
   * @return true if the environment configuration is enabled, false otherwise.
   */
  @Override
  public boolean isEnabled() {
    return this.enabled;
  }

  public String getOntology() {
    return environmentConfig.flatMap(c -> JsonObjectUtils.getString(c, "ontology", LOGGER::error))
                            .orElse(null);
  }

}
