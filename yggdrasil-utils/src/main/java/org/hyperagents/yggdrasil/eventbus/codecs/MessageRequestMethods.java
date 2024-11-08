package org.hyperagents.yggdrasil.eventbus.codecs;

import java.util.Arrays;
import java.util.Optional;

enum MessageRequestMethods {
  GET_ENTITY("getEntity"),
  GET_ENTITY_IRI("getEntityIri"),
  UPDATE_ENTITY("updateEntity"),
  PATCH_ENTITY("patchEntity"),
  DELETE_ENTITY("deleteEntity"),
  CREATE_WORKSPACE("createWorkspace"),
  CREATE_SUB_WORKSPACE("createSubWorkspace"),
  JOIN_WORKSPACE("joinWorkspace"),
  LEAVE_WORKSPACE("leaveWorkspace"),
  FOCUS("focus"),
  CREATE_ARTIFACT("createArtifact"),
  CREATE_BODY("createBody"),
  DO_ACTION("performAction"),
  GET_WORKSPACES("GetWorkspaces"),
    GET_ARTIFACTS("GetArtifacts"),
  QUERY("query");

  private static final String PREFIX = "org.hyperagents.yggdrasil.eventbus.methods.";

  private final String name;

  MessageRequestMethods(final String name) {
    this.name = PREFIX + name;
  }

  public String getName() {
    return this.name;
  }

  public static Optional<MessageRequestMethods> getFromName(final String name) {
    return Arrays.stream(values()).filter(m -> m.getName().equals(name)).findFirst();
  }
}
