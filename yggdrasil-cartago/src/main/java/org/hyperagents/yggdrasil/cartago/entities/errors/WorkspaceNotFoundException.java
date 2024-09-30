package org.hyperagents.yggdrasil.cartago.entities.errors;

/**
 * Exception thrown when a workspace is not found.
 */
@SuppressWarnings("serial")
public class WorkspaceNotFoundException extends Exception {
  public WorkspaceNotFoundException(final String message) {
    super(message);
  }
}

