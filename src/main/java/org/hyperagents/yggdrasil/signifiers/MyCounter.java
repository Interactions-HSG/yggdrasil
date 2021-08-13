package org.hyperagents.yggdrasil.signifiers;

import cartago.OPERATION;
import cartago.ObsProperty;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifact;

public class MyCounter extends HypermediaArtifact {

  public void init() {
    System.out.println("init");
    defineObsProperty("count", 0);
  }

  @OPERATION
  public void inc() {
    ObsProperty prop = getObsProperty("count");
    prop.updateValue(prop.intValue() + 1);
  }

  @Override
  protected void registerInteractionAffordances() {
    // Register one action affordance with an input schema
    registerActionAffordance("http://example.org/Increment", "inc", "/increment");
  }
}
