package org.hyperagents.yggdrasil.messages;

public enum MessageAddresses {
  RDF_STORE("rdfstore"),
  CARTAGO("cartago"),
  HTTP_NOTIFICATION_DISPATCHER("dispatcher");

  private static final String PREFIX = "org.hyperagents.yggdrasil.eventbus.";

  private final String name;

  MessageAddresses(final String name) {
    this.name = PREFIX + name;
  }

  public String getName() {
    return this.name;
  }
}
