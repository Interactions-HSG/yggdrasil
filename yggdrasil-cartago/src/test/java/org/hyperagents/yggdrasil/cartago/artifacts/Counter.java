package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_CLASS_NAMES")
public class Counter extends HypermediaArtifact {

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

  @Override
  protected void registerInteractionAffordances() {
    // Register one action affordance with an input schema
    this.registerActionAffordance("http://example.org/Increment", "inc", "/increment");
  }
}
