package org.hyperagents.yggdrasil.cartago;

import cartago.OPERATION;
import cartago.ObsProperty;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.IntegerSchema;

public class Counter extends HypermediaArtifact {
  
  public void init() {
    defineObsProperty("count", 0);
  }
  
  @OPERATION
  public void inc(int counter) {
    ObsProperty prop = getObsProperty("count");
    prop.updateValue(prop.intValue()+1);
    signal("tick", counter);
  }
  
  @Override
  protected void registerInteractionAffordances() {
    // Register one action affordance with an input schema
    registerActionAffordance("inc", "/increment", new IntegerSchema.Builder().build());
  }
}
