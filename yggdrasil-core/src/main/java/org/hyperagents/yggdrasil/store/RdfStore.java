package org.hyperagents.yggdrasil.store;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import java.util.Optional;

public interface RdfStore {
  boolean containsEntityModel(IRI entityIri);

  Optional<Model> getEntityModel(IRI entityIri);

  void addEntityModel(IRI entityIri, Model entityModel);

  void replaceEntityModel(IRI entityIri, Model entityModel);

  void removeEntityModel(IRI entityIri);
}
