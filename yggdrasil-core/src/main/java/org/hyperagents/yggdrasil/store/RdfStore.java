package org.hyperagents.yggdrasil.store;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;

public interface RdfStore {
  boolean containsEntityModel(IRI entityIri);

  Optional<Model> getEntityModel(IRI entityIri);

  void addEntityModel(IRI entityIri, Model entityModel);

  void replaceEntityModel(IRI entityIri, Model entityModel);

  void removeEntityModel(IRI entityIri);

  void close();

  Set<Map<String, Optional<String>>> queryGraph(String query)
      throws IllegalArgumentException, IOException;
}
