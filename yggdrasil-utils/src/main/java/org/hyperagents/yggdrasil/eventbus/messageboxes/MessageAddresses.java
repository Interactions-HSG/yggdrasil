package org.hyperagents.yggdrasil.eventbus.messageboxes;

/**
 * Represents the message addresses used in the Yggdrasil event bus.
 */
enum MessageAddresses {
  RDF_STORE("rdfstore"),
  CARTAGO("cartago"),
  HTTP_NOTIFICATION_DISPATCHER("dispatcher");

  private static final String PREFIX = "org.hyperagents.yggdrasil.eventbus.";

  private final String name;

  /**
   * Constructs a new MessageAddresses enum with the specified name.
   *
   * @param name the name of the message address
   */
  MessageAddresses(final String name) {
    this.name = PREFIX + name;
  }

  /**
   * Returns the name of the message address.
   *
   * @return the name of the message address
   */
  String getName() {
    return this.name;
  }
}
