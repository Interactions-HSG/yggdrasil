package org.hyperagents.yggdrasil.messages;

public enum MessageNotifications {
  ENTITY_CREATED("entityCreated"),
  ENTITY_CHANGED("entityChanged"),
  ENTITY_DELETED("entityDeleted"),
  ARTIFACT_OBS_PROP("artifactObsProp");

  private static final String PREFIX = "org.hyperagents.yggdrasil.eventbus.notifications.";

  private final String name;

  MessageNotifications(final String name) {
    this.name = PREFIX + name;
  }

  public String getName() {
    return this.name;
  }
}
