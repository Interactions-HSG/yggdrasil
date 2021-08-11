package org.hyperagents.yggdrasil.signifiers;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifact;

import java.io.*;

public class AgentProfileArtifact extends HypermediaArtifact {

  AgentProfile profile;

  public void init(Resource agent){
    profile = new AgentProfile(agent);
  }

  @OPERATION
  public void getAgentProfile(OpFeedbackParam<Object> returnParam){
    returnParam.set(profile);

  }

  @OPERATION
  public void rewrite(String s) {
    Model model = new ModelBuilder().build();
    RDFHandler handler = new StatementCollector(model);
    RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
    parser.setRDFHandler(handler);
    InputStream stream = new ByteArrayInputStream(s.getBytes());
    try {
      parser.parse(stream, "http://example.com/");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @OPERATION
  public void rewrite(Model m){
    this.profile.rewrite(m);
  }

  @Override
  protected void registerInteractionAffordances() {
      registerActionAffordance("http://example.com/write", "rewrite", "/rewrite", new StringSchema.Builder().build());
    }


}
