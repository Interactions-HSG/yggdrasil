package org.hyperagents.yggdrasil.store.impl;

import io.vertx.core.json.JsonObject;
import org.hyperagents.yggdrasil.store.RdfStore;

public class RdfStoreFactory {

  public static RdfStore createStore(JsonObject config) {
    return new Rdf4jStore(config);
  }
}
