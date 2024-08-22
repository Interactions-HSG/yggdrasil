package org.hyperagents.yggdrasil.store;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;

public interface RdfStore {
  boolean containsEntityModel(IRI entityIri) throws IOException;

  Optional<Model> getEntityModel(IRI entityIri) throws IOException;

  void addEntityModel(IRI entityIri, Model entityModel) throws IOException;

  void replaceEntityModel(IRI entityIri, Model entityModel) throws IOException;

  void updateEntityModel(IRI entityIri, Model metaData) throws IOException;

  void removeEntityModel(IRI entityIri) throws IOException;

  void close() throws IOException;

  String queryGraph(
      String query,
      List<String> defaultGraphUris,
      List<String> namedGraphUris,
      String responseContentType
  ) throws IllegalArgumentException, IOException;
}
