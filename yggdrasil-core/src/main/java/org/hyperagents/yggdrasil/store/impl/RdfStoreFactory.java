package org.hyperagents.yggdrasil.store.impl;

import java.io.File;
import java.io.IOException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.hyperagents.yggdrasil.store.RdfStore;

public final class RdfStoreFactory {

  private RdfStoreFactory() {}

  public static RdfStore createInMemoryStore() throws IOException {
    return new Rdf4jStore(new MemoryStore());
  }

  public static RdfStore createFilesystemStore(final String storePath) throws IOException {
    return new Rdf4jStore(new NativeStore(new File(storePath)));
  }
}
