package org.hyperagents.yggdrasil.eventbus.codecs;

import java.util.Arrays;
import java.util.Optional;

enum MessageNotifications {
  ENTITY_CREATED("entityCreated"),
  ENTITY_CHANGED("entityChanged"),
  ENTITY_DELETED("entityDeleted"),
  ARTIFACT_OBS_PROP("artifactObsProp"),
  ACTION_REQUESTED("actionRequested"),
  ACTION_SUCCEEDED("actionSucceeded"),
  ACTION_FAILED("actionFailed");

  private static final String PREFIX = "org.hyperagents.yggdrasil.eventbus.notifications.";

  private final String name;

  MessageNotifications(final String name) {
    this.name = PREFIX + name;
  }

  public String getName() {
    return this.name;
  }

  public static Optional<MessageNotifications> getFromName(final String name) {
    return Arrays.stream(values()).filter(n -> n.getName().equals(name)).findFirst();
  }
}
