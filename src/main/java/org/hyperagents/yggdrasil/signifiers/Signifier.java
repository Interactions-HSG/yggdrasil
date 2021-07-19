package org.hyperagents.yggdrasil.signifiers;


import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import java.io.StringReader;

public class Signifier {
  private Resource signifierId;
  private Model model;

  protected Signifier(Resource signifierId, Model model) {
    this.signifierId = signifierId;
    this.model = model;
  }

  public Signifier(String signifierName, String signifierContent, String baseURI){
    ValueFactory rdf = SimpleValueFactory.getInstance();
    signifierId = rdf.createIRI(signifierName);
    model = createModel(signifierContent, baseURI);

  }

  private Model createModel(String content, String baseURI){
    Model model = new LinkedHashModel();
    StringReader reader = new StringReader(content);
    RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
    parser.setRDFHandler(new StatementCollector(model));
    try {
      parser.parse(reader, baseURI);
    } catch( Exception e){
      e.printStackTrace();
    }
    return model;

  }

  public Resource getSignifierId() {
    return signifierId;
  }

  public Model getModel() {
    return model;
  }
}
