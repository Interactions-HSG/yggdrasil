package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.hmas.interaction.shapes.ValueSpecification;
import java.util.List;

/**
 * IntellIotArtifact.
 */
public class IntellIotHMASArtifact extends HypermediaHMASArtifact {

  /**
   * normalizes some values.
   *
   * @param alpha alpha
   * @param x x
   * @param y y
   * @param newValues new values
   */
  @OPERATION
  public void normalizeValues(
      final double alpha,
      final double x,
      final double y,
      final OpFeedbackParam<List<Double>> newValues
  ) {
    newValues.set(List.of(
        normalizeBoundaries(alpha, -20, 25),
        normalizeBoundaries(x / 1000, 0.08, 1.05),
        normalizeBoundaries(y / 1000, 0.365, 0.5)
    ));
  }

  private double normalizeBoundaries(final double x, final double low, final double high) {
    return Math.max(low, Math.min(x, high));
  }

  @OPERATION
  public void generateRandomId(final int c, final OpFeedbackParam<Integer> p) {
    p.set((int) Math.floor(c * Math.random()));
  }

  // TODO: Set correct Input
  /*
          new ArraySchema.Builder()
                       .addItem(new NumberSchema.Builder().build())
                       .addItem(new NumberSchema.Builder().build())
                       .addItem(new NumberSchema.Builder().build())
                       .build()
   */
  protected void registerInteractionAffordances() {
    this.registerSignifier(
        "normalizeValues",
        "normalizeValues",
        "/normalizeValues",
        new ValueSpecification.Builder().build()
    );
    this.registerFeedbackParameter("normalizeValues");
    /*
            new ArraySchema.Builder()
                       .addItem(new IntegerSchema.Builder().build())
                       .build()
     */
    this.registerSignifier(
        "generateRandomId",
        "generateRandomId",
        "/generateRandomId",
        new ValueSpecification.Builder().build()

    );
    this.registerFeedbackParameter("generateRandomId");
  }
}
