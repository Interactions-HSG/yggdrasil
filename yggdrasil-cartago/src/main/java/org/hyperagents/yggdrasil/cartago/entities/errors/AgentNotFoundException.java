package org.hyperagents.yggdrasil.cartago.entities.errors;

/**
 * Exception thrown when an agent is not found.
 */
public class AgentNotFoundException extends Exception {
  public AgentNotFoundException(String message) {
    super(message);
  }
}
