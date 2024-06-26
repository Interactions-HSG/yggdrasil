package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.IntegerSchema;

import java.util.Random;


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

  @OPERATION
  public void rand(final OpFeedbackParam<Integer> randomInt) {
    Random rand = new Random();
    int randInt = rand.nextInt();
    System.out.println("Random Integer: " + randInt);
    randomInt.set(randInt);
  }

  @OPERATION
  public void rand2(final OpFeedbackParam<Integer> randInt1, final OpFeedbackParam<Integer> randInt2) {
    Random rand = new Random();
    int one = rand.nextInt();
    int two = rand.nextInt();
    System.out.println("one: " + one + " two: " + two);
    randInt1.set(one);
    randInt2.set(two);
  }

  @Override
  protected void registerInteractionAffordances() {
    this.registerActionAffordance(
      "http://example.org/egcd",
      "egcd",
      "egcd",
      new ArraySchema.Builder()
        .addItem(new IntegerSchema.Builder().build())
        .addItem(new IntegerSchema.Builder().build())
        .build()
    );
    this.registerFeedbackParameters("egcd",3);
    this.registerActionAffordance(
      "http://example.org/rand",
      "rand",
      "rand"
    );
    this.registerFeedbackParameters("rand",1);
    this.registerActionAffordance(
      "http://example.org/rand2",
      "rand2",
      "rand2"
    );
    this.registerFeedbackParameters("rand2",2);
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
