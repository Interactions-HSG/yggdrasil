package org.hyperagents.yggdrasil.eventbus.messageboxes;

/**
 * TODO: Javadoc.
 */
enum MessageAddresses {
  RDF_STORE("rdfstore"),
  CARTAGO("cartago"),
  HTTP_NOTIFICATION_DISPATCHER("dispatcher");

  private static final String PREFIX = "org.hyperagents.yggdrasil.eventbus.";

  private final String name;

  /**
   * TODO: Javadoc.
   */
  MessageAddresses(final String name) {
    this.name = PREFIX + name;
  }

  /**
   * TODO: Javadoc.
   */
  String getName() {
    return this.name;
  }
}
