package ro.andreiciortea.yggdrasil.store.impl;

import ro.andreiciortea.yggdrasil.store.RdfStore;

public class RdfStoreFactory {

  public static RdfStore createStore(String storeIdentifier) {
    return new Rdf4jStore();
  }
}
