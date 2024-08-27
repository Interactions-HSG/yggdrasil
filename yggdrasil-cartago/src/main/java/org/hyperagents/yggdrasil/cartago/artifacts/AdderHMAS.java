package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.hmas.interaction.shapes.IntegerSpecification;
import ch.unisg.ics.interactions.hmas.interaction.shapes.ListSpecification;

/**
 * This class represents a Hypermedia HMAS Artifact with addition functionality.
 */
public class AdderHMAS extends HypermediaHMASArtifact {

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
   * This method registers the interaction affordances for the AdderHMAS artifact.
   */
  @Override
  protected void registerInteractionAffordances() {
    this.registerSignifier(
        "http://example.org/Add",
        "add",
        "add",
      new ListSpecification.Builder()
        .setIRIAsString("http://example.org/addends")
        .setRequired(true)
        .addMemberSpecification(
          new IntegerSpecification.Builder()
            .setRequired(true)
            .setName("1st Parameter")
            .build()
        )
        .addMemberSpecification(
          new IntegerSpecification.Builder()
            .setRequired(true)
            .setName("2nd Parameter")
            .build()
        )
        .build(),
      new ListSpecification.Builder()
        .setIRIAsString("http://example.org/result")
        .setRequired(true)
        .addMemberSpecification(
          new IntegerSpecification.Builder()
            .setRequired(true)
            .setName("Result")
            .build()
        )
        .build()
    );
  }
}
