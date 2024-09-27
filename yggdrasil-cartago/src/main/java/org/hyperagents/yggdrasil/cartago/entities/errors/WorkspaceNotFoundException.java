package org.hyperagents.yggdrasil.cartago.entities.errors;

/**
 * Exception thrown when a workspace is not found.
 */
public class WorkspaceNotFoundException extends Exception {
  public WorkspaceNotFoundException(String message) {
    super(message);
  }
}

