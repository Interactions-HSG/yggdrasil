package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.IntegerSchema;

/**
 * This method registers the interaction affordances for the AdderHMAS artifact.
 */
public class AdderTD extends HypermediaTDArtifact {

  /**
   * This operation performs the addition of two integers.
   *
   * @param x The first integer to add.
   * @param y The second integer to add.
   * @param sumParam The parameter to store the result of the addition.
   */
  @OPERATION
  public void add(final int x, final int y, final OpFeedbackParam<Integer> sumParam) {
    this.log("adder performs add");
    sumParam.set(x + y);
    this.log("result in adder: " + sumParam.get());
  }

  /**
   * This method registers the interaction affordances for the AdderTD artifact.
   */
  @Override
  protected void registerInteractionAffordances() {
    this.registerActionAffordance(
      "http://example.org/add",
      "add",
      "add",
      new ArraySchema.Builder()
        .addItem(new IntegerSchema.Builder().build())
        .addItem(new IntegerSchema.Builder().build())
        .build(),
      new ArraySchema.Builder()
        .addItem(new IntegerSchema.Builder().build())
        .build()
    );
  }
}
