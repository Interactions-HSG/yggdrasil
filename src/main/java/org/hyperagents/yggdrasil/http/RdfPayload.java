package org.hyperagents.yggdrasil.http;

import java.io.IOException;
import java.io.StringReader;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

public class RdfPayload {
  private static final String INDIVIDUAL_URI = "http://example.org/my_individual";
  private final Model model;
  
  public RdfPayload(RDFFormat format, String payload, String baseUri) throws RDFParseException, 
      RDFHandlerException, IOException {
    payload = payload.replaceAll("<>", "<" + INDIVIDUAL_URI + ">");
    this.model = readModelFromString(format, payload, baseUri);
  }
  
  public Set<IRI> getSemanticTypes() {
    Set<IRI> types = Models.objectIRIs(model.filter(SimpleValueFactory.getInstance()
        .createIRI(INDIVIDUAL_URI), RDF.TYPE, null));
    
    return types;
  }
  
  private Model readModelFromString(RDFFormat format, String description, String baseUri) 
      throws RDFParseException, RDFHandlerException, IOException {
    StringReader stringReader = new StringReader(description);
    
    RDFParser rdfParser = Rio.createParser(format);
    Model model = new LinkedHashModel();
    rdfParser.setRDFHandler(new StatementCollector(model));
    
    rdfParser.parse(stringReader, baseUri);
    
    return model;
  }
}
