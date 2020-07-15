package org.hyperagents.yggdrasil.cartago;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.ObsProperty;

public class SimpleCounter extends Artifact {
  
  public void init() {
    defineObsProperty("count", 0);
  }
  
  @OPERATION
  public void inc(int counter) {
    ObsProperty prop = getObsProperty("count");
    prop.updateValue(prop.intValue() + 1);
    signal("tick");
  }
  
}
