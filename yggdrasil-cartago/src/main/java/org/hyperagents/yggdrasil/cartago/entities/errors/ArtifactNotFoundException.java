package org.hyperagents.yggdrasil.cartago.entities.errors;

/**
 * Exception thrown when an artifact is not found.
 */
@SuppressWarnings("serial")
public class ArtifactNotFoundException extends Exception {
  public ArtifactNotFoundException(final String message) {
    super(message);
  }
}
