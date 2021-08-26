package org.hyperagents.yggdrasil.signifiers;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.hyperagents.signifier.Signifier;
import org.hyperagents.util.RDFS;

public class SignifierTest extends SignifierHypermediaArtifact {

  public void init(){
    Resource signifierId = RDFS.rdf.createBNode("signifier");
    Signifier signifier = new Signifier.Builder(signifierId).build();
    addSignifier(signifier);
  }

  @Override
  public Model getState() {
    Model model = new ModelBuilder().build();
    return model;
  }

  @Override
  protected void registerInteractionAffordances() {
    registerSignifierAffordances();

  }
}

