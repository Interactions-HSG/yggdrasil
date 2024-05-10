package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_CLASS_NAMES")
public class CounterHMAS extends HypermediaHMASArtifact {

  public void init() {
    this.defineObsProperty("count", 0);
  }

  public void init(final int count) {
    this.defineObsProperty("count", count);
  }

  @OPERATION
  public void inc() {
    final var prop = this.getObsProperty("count");
    prop.updateValue(prop.intValue() + 1);
    System.out.println("count incremented");
  }

  @OPERATION
  public void sign() {
    signal("tick");
    System.out.println("tick");
  }

  @Override
  protected void registerInteractionAffordances() {
    // Register one action affordance with an input schema
    this.registerSignifier("http://example.org/Increment", "inc", "increment");
    this.registerSignifier("http://example.org/Sign","sign","sign");
  }
}
