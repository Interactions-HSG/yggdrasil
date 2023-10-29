package org.hyperagents.yggdrasil.messages;

import java.util.Arrays;
import java.util.Optional;

public enum MessageRequestMethods {
  GET_ENTITY("getEntity"),
  CREATE_ENTITY("createEntity"),
  UPDATE_ENTITY("updateEntity"),
  PATCH_ENTITY("patchEntity"),
  DELETE_ENTITY("deleteEntity"),
  CREATE_WORKSPACE("createWorkspace"),
  CREATE_ARTIFACT("instantiateArtifact"),
  DO_ACTION("performAction");


  private static final String PREFIX = "org.hyperagents.yggdrasil.eventbus.methods.";

  private final String name;

  MessageRequestMethods(final String name) {
    this.name = PREFIX + name;
  }

  public String getName() {
    return this.name;
  }

  public static Optional<MessageRequestMethods> getFromName(final String name) {
    return Arrays.stream(MessageRequestMethods.values()).filter(m -> m.getName().equals(name)).findFirst();
  }
}
