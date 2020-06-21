package org.hyperagents.yggdrasil.cartago;

import cartago.OPERATION;
import cartago.ObsProperty;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
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
    registerActionAffordance("inc", new ActionAffordance.Builder(
          new Form.Builder(getArtifactUri() + "/increment")
            .build())
        .addTitle("inc")
        .addInputSchema(new ArraySchema.Builder()
          .addMinItems(1)
          .addMaxItems(1)
          .addItem(new IntegerSchema.Builder().build())
          .build())
        .build());
  }
}
