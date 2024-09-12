package org.hyperagents.yggdrasil.utils.impl;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.JsonObjectUtils;

/**
 * Represents an implementation of the EnvironmentConfig interface.
 * This class provides functionality to retrieve and manage environment configuration settings.
 */
public class EnvironmentConfigImpl implements EnvironmentConfig {
  private static final Logger LOGGER = LogManager.getLogger(EnvironmentConfigImpl.class);

  private final boolean enabled;

  private final JsonObject environmentConfig;

  /**
   * Constructs a new EnvironmentConfigImpl object with the specified configuration.
   *
   * @param config The JSON object containing the environment configuration settings.
   */
  public EnvironmentConfigImpl(final JsonObject config) {
    environmentConfig =
        JsonObjectUtils.getJsonObject(config, "environment-config", LOGGER::error)
            .orElse(null);
    this.enabled = environmentConfig != null
        &&
        JsonObjectUtils.getBoolean(environmentConfig, "enabled", LOGGER::error)
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

  /**
   * Returns the Ontology chosen for the current environment.
   *
   * @return String of the ontology
   */
  public String getOntology() {
    return environmentConfig != null
        ?
        JsonObjectUtils.getString(environmentConfig, "ontology", LOGGER::error)
            .orElse(null) : null;

  }

}
