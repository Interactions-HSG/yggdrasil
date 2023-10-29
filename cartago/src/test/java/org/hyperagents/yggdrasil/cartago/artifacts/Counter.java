package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import cartago.ObsProperty;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifact;

public class Counter extends HypermediaArtifact {

  public void init() {
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
