package ro.andreiciortea.yggdrasil.store;

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;

public interface RdfStore {

  boolean containsEntityGraph(IRI entityIRI);
  
  Optional<Graph> getEntityGraph(IRI entityIRI);
  
  void createEntityGraph(IRI entityIRI, Graph entityGraph) throws IllegalArgumentException, IOException;;
  
  Optional<Graph> patchEntityGraph(IRI entityIRI, Graph addedTriples, Graph removedTriples);
  
  Optional<Graph> updateEntityGraph(IRI entityIRI, Graph entityGraph);
  
  Optional<Graph> deleteEntityGraph(IRI entityIRI);
  
  IRI createIRI(String iriString) throws IllegalArgumentException;
  
  String graphToString(Graph graph, RDFSyntax syntax) throws IllegalArgumentException, IOException;
  
  Graph stringToGraph(String graphString, IRI baseIRI, RDFSyntax syntax) throws IllegalArgumentException, IOException;
  
}
