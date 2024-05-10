package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.IntegerSchema;


public class MathTD extends HypermediaTDArtifact {
  @OPERATION
  public void egcd(final int a, final int b, final OpFeedbackParam<Integer> gcd, final OpFeedbackParam<Integer> x,final OpFeedbackParam<Integer> y) {
    this.log("Calculating egcd of " + a + " and " + b);
    var temp = extendedEuclidean(a,b);
    gcd.set(temp[0]);
    x.set(temp[1]);
    y.set(temp[2]);
    this.log("GCD: "+ gcd.get() +",x: " + x.get() + ",y: " + y.get());
  }

  @Override
  protected void registerInteractionAffordances() {
    this.registerActionAffordance(
      "http://example.org/egcd",
      "egcd",
      "/egcd",
      new ArraySchema.Builder()
        .addItem(new IntegerSchema.Builder().build())
        .addItem(new IntegerSchema.Builder().build())
        .build()
    );
    this.registerFeedbackParameters("egcd",3);
  }

  public static int[] extendedEuclidean(int a, int b) {
    if (b == 0) {
      return new int[] {a, 1, 0};
    } else {
      int[] arr = extendedEuclidean(b, a % b);
      int gcd = arr[0];
      int x = arr[2];
      int y = arr[1] - (a / b) * arr[2];
      return new int[] {gcd, x, y};
    }
  }
}
