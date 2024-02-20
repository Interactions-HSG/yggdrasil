package org.hyperagents.yggdrasil.utils;

import io.vertx.core.shareddata.Shareable;

/**
 * Represents the configuration for WebSub.
 */
public interface WebSubConfig extends Shareable {
  
  /**
   * Checks if WebSub is enabled.
   *
   * @return true if WebSub is enabled, false otherwise.
   */
  boolean isEnabled();

  /**
   * Gets the WebSub hub URI.
   *
   * @return the WebSub hub URI.
   */
  String getWebSubHubUri();
}
