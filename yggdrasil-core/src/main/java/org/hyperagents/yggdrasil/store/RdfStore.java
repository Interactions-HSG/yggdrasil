package org.hyperagents.yggdrasil.store;

import java.io.IOException;
import java.util.Optional;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;

public interface RdfStore {
  boolean containsEntityGraph(IRI entityIri);

  Optional<Graph> getEntityGraph(IRI entityIri);

  void createEntityGraph(IRI entityIri, Graph entityGraph) throws IllegalArgumentException;

  void updateEntityGraph(IRI entityIri, Graph entityGraph) throws IllegalArgumentException;

  void deleteEntityGraph(IRI entityIri);

  void addEntityGraph(IRI entityIri, Graph entityGraph);
}
