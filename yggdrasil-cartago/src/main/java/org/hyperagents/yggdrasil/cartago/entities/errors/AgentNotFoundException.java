package org.hyperagents.yggdrasil.cartago.entities.errors;

/**
 * Exception thrown when an agent is not found.
 */
@SuppressWarnings("serial")
public class AgentNotFoundException extends Exception {
  public AgentNotFoundException(final String message) {
    super(message);
  }

}
