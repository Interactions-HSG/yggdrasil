package org.hyperagents.yggdrasil.utils;

import io.vertx.core.shareddata.Shareable;

public interface EnvironmentConfig extends Shareable {
  boolean isEnabled();
}
