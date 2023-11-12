package org.hyperagents.yggdrasil.store.impl;

import io.vertx.core.json.JsonObject;
import org.hyperagents.yggdrasil.store.RdfStore;

public final class RdfStoreFactory {

  private RdfStoreFactory() {}

  public static RdfStore createStore(final JsonObject config) {
    return new Rdf4jStore(config);
  }
}
