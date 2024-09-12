package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.hmas.interaction.shapes.IntegerSpecification;
import ch.unisg.ics.interactions.hmas.interaction.shapes.ListSpecification;
import java.util.Random;


/**
 * Basic artifact that illustrates how to write operations with multiple Feedbackparams.
 */
public class MathHMAS extends HypermediaHMASArtifact {

  private static final Random random = new Random();

  /**
   * Takes two integers and returns the extended euclidian algorithm.
   *
   * @param a first integer.
   * @param b second integer.
   * @return an Array of 3 integers.
   */
  public static int[] extendedEuclidean(final int a, final int b) {
    if (b == 0) {
      return new int[] {a, 1, 0};
    } else {
      final int[] arr = extendedEuclidean(b, a % b);
      final int gcd = arr[0];
      final int x = arr[2];
      final int y = arr[1] - (a / b) * arr[2];
      return new int[] {gcd, x, y};
    }
  }

  /**
   * The operation that is callable through cartago and http requests. Is a wrapper function around
   * the private gcd method and assigns the result to outputparams.
   *
   * @param a   first Integer.
   * @param b   second Integer.
   * @param gcd the resulting GCD.
   * @param x   the resulting x value.
   * @param y   the resuling y value.
   */
  @OPERATION
  public void egcd(final int a, final int b, final OpFeedbackParam<Integer> gcd,
                   final OpFeedbackParam<Integer> x, final OpFeedbackParam<Integer> y) {
    this.log("Calculating egcd of " + a + " and " + b);
    final var temp = extendedEuclidean(a, b);
    gcd.set(temp[0]);
    x.set(temp[1]);
    y.set(temp[2]);
    this.log("GCD: " + gcd.get() + ",x: " + x.get() + ",y: " + y.get());
  }

  /**
   * Returns a random Integer.
   *
   * @param randomInt output variable.
   */
  @OPERATION
  public void rand(final OpFeedbackParam<Integer> randomInt) {
    final int randInt = random.nextInt();
    System.out.println("Random int: " + randInt);
    randomInt.set(randInt);
  }

  /**
   * Returns two random Integers.
   *
   * @param randInt1 output variable one.
   * @param randInt2 output variable two.
   */
  @OPERATION
  public void rand2(final OpFeedbackParam<Integer> randInt1,
                    final OpFeedbackParam<Integer> randInt2) {
    final int one = random.nextInt();
    final int two = random.nextInt();
    System.out.println("one: " + one + " two: " + two);
    randInt1.set(one);
    randInt2.set(two);
  }

  @Override
  protected void registerInteractionAffordances() {
    this.registerSignifier(
        "http://example.org/egcd",
        "egcd",
        "egcd",
        new ListSpecification.Builder()
            .setIRIAsString("http://example.org/egcd")
            .setRequired(true)
            .addMemberSpecification(new IntegerSpecification.Builder()
                .setName("a")
                .setRequired(true)
                .build())
            .addMemberSpecification(new IntegerSpecification.Builder()
                .setName("b")
                .setRequired(true)
                .build())
            .build(),
        new ListSpecification.Builder()
            .setIRIAsString("http://example.org/egcdResult")
            .setRequired(true)
            .addMemberSpecification(new IntegerSpecification.Builder()
                .setName("gcd")
                .setRequired(true)
                .build())
            .addMemberSpecification(new IntegerSpecification.Builder()
                .setName("x")
                .setRequired(true)
                .build())
            .addMemberSpecification(new IntegerSpecification.Builder()
                .setName("y")
                .setRequired(true)
                .build())
            .build()
    );
    this.registerSignifier(
        "http://example.org/rand",
        "rand",
        "rand",
        null,
        new ListSpecification.Builder()
            .setIRIAsString("http://example.org/randInt")
            .setRequired(true)
            .addMemberSpecification(new IntegerSpecification.Builder()
                .setName("randomInteger")
                .setRequired(true)
                .build())
            .build()
    );
    this.registerSignifier(
        "http://example.org/rand2",
        "rand2",
        "rand2",
        null,
        new ListSpecification.Builder()
            .setIRIAsString("http://example.org/randInts")
            .setRequired(true)
            .addMemberSpecification(new IntegerSpecification.Builder()
                .setName("randomInteger1")
                .setRequired(true)
                .build())
            .addMemberSpecification(new IntegerSpecification.Builder()
                .setName("randomInteger2")
                .setRequired(true)
                .build())
            .build()
    );
  }
}
