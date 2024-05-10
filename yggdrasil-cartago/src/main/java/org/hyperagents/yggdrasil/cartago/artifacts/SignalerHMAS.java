package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_CLASS_NAMES")
public class SignalerHMAS extends HypermediaHMASArtifact {

  public void init() {}

  @OPERATION
  public void sign() {
    signal("tick");
    System.out.println("tick");
  }

  @Override
  protected void registerInteractionAffordances() {
    // Register one action affordance with an input schema
    this.registerSignifier("http://example.org/Sign","sign","sign");
  }
}
