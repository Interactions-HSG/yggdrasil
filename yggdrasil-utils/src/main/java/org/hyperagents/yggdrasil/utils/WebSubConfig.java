package org.hyperagents.yggdrasil.utils;

import io.vertx.core.shareddata.Shareable;

/**
 * TODO: Javadoc.
 */
public interface WebSubConfig extends Shareable {
  boolean isEnabled();

  String getWebSubHubUri();
}
