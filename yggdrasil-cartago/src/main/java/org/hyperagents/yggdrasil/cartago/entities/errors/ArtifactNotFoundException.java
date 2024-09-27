package org.hyperagents.yggdrasil.cartago.entities.errors;

/**
 * Exception thrown when an artifact is not found.
 */
public class ArtifactNotFoundException extends Exception {
  public ArtifactNotFoundException(String message) {
    super(message);
  }
}
