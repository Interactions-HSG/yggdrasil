package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.IntegerSchema;
import ch.unisg.ics.interactions.wot.td.schemas.NumberSchema;
import java.util.List;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifact;

public class IntellIotArtifact extends HypermediaArtifact {

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

  protected void registerInteractionAffordances() {
    this.registerActionAffordance(
        "normalizeValues",
        "normalizeValues",
        "/normalizeValues",
        new ArraySchema.Builder()
                       .addItem(new NumberSchema.Builder().build())
                       .addItem(new NumberSchema.Builder().build())
                       .addItem(new NumberSchema.Builder().build())
                       .build()
    );
    this.registerFeedbackParameter("normalizeValues");
    this.registerActionAffordance(
        "generateRandomId",
        "generateRandomId",
        "/generateRandomId",
        new ArraySchema.Builder()
                       .addItem(new IntegerSchema.Builder().build())
                       .build()
    );
    this.registerFeedbackParameter("generateRandomId");
  }
}
