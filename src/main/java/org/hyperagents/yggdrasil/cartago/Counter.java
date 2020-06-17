package org.hyperagents.yggdrasil.cartago;

import org.hyperagents.yggdrasil.core.HypermediaArtifactRegistry;

import cartago.OPERATION;
import cartago.ObsProperty;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;

public class Counter extends HypermediaArtifact {
  
  public void init() {
    HypermediaArtifactRegistry.getInstance().register(this);
    
    defineObsProperty("count", 0);
  }
  
  @OPERATION
  public void inc() {
    ObsProperty prop = getObsProperty("count");
    prop.updateValue(prop.intValue()+1);
    signal("tick");
  }
  
  @Override
  public String getSemanticType() {
    return "http://example.org/Counter";
  }
  
  @Override
  protected void collectActionAffordances() {
    exposeActionAffordance("inc", new ActionAffordance.Builder(
        new Form.Builder(getArtifactUri() + "/increment")
            .build())
        .build());
  }
}
