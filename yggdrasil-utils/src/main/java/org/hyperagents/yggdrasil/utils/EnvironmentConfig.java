package org.hyperagents.yggdrasil.utils;

import io.vertx.core.shareddata.Shareable;

/**
 * Represents the configuration for the environment.
 * This interface extends the Shareable interface from the Vert.x library.
 * It provides a method to check if the environment is enabled.
 */
public interface EnvironmentConfig extends Shareable {
  /**
   * Checks if the environment is enabled.
   *
   * @return true if the environment is enabled, false otherwise.
   */
  boolean isEnabled();
}
