package org.hyperagents.yggdrasil.signifiers.artifacts;

import cartago.OPERATION;
import cartago.ObsProperty;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.hyperagents.yggdrasil.signifiers.SignifierHypermediaArtifact;

public class SignifierCounter extends SignifierHypermediaArtifact {

  public void init() {
    System.out.println("start init");
    defineObsProperty("count", 0);
  }


  @OPERATION
  public void inc() {
    ObsProperty prop = getObsProperty("count");
    prop.updateValue(prop.intValue() + 1);
  }

  @Override
  public Model getState() {
    Model model = new ModelBuilder().build();
    return model;
  }


  @Override
  protected void registerInteractionAffordances() {
    System.out.println("is registering");
    registerSignifierAffordances();
    registerActionAffordance("http://example.org/Increment", "inc", "/increment");

  }
}
