package org.hyperagents.yggdrasil.store;

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;

public interface RdfStore {
  public static final String GET_ENTITY = "methods.getEntity";
  public static final String CREATE_ENTITY = "methods.createEntity";
  public static final String UPDATE_ENTITY = "methods.updateEntity";
  public static final String PATCH_ENTITY = "methods.patchEntity";
  public static final String DELETE_ENTITY = "methods.deleteEntity";
  
  boolean containsEntityGraph(IRI entityIRI);

  Optional<Graph> getEntityGraph(IRI entityIRI);

  void createEntityGraph(IRI entityIRI, Graph entityGraph) throws IllegalArgumentException;

  void updateEntityGraph(IRI entityIRI, Graph entityGraph) throws IllegalArgumentException;

  void deleteEntityGraph(IRI entityIRI);

  void addEntityGraph(IRI entityIri, Graph entityGraph);

  IRI createIRI(String iriString) throws IllegalArgumentException;

  String graphToString(Graph graph, RDFSyntax syntax) throws IllegalArgumentException, IOException;

  Graph stringToGraph(String graphString, IRI baseIRI, RDFSyntax syntax) throws IllegalArgumentException, IOException;

}
