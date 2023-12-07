package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.IntegerSchema;

public class Adder extends HypermediaArtifact {
  @OPERATION
  public void add(final int x, final int y, final OpFeedbackParam<Integer> sumParam) {
    this.log("adder performs add");
    sumParam.set(x + y);
    this.log("result in adder: " + sumParam.get());
  }

  @Override
  protected void registerInteractionAffordances() {
    this.registerActionAffordance(
        "http://example.org/add",
        "add",
        "/add",
        new ArraySchema.Builder()
                       .addItem(new IntegerSchema.Builder().build())
                       .addItem(new IntegerSchema.Builder().build())
                       .build()
    );
    this.registerFeedbackParameter("add");
  }
}
