package org.hyperagents.yggdrasil.store.impl;

import org.hyperagents.yggdrasil.store.RdfStore;

public class RdfStoreFactory {

  public static RdfStore createStore(String storeIdentifier) {
    return new Rdf4jStore();
  }
}
