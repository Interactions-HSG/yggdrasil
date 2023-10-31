package org.hyperagents.yggdrasil.eventbus.codecs;

enum MessageFields {
  REQUEST_METHOD("requestMethod"),
  REQUEST_URI("requestUri"),
  ENTITY_URI_HINT("slug"),
  CONTENT_TYPE("contentType"),
  AGENT_ID("agentID"),
  ENV_NAME("envName"),
  WORKSPACE_NAME("workspaceName"),
  ARTIFACT_NAME("artifactName"),
  ACTION_NAME("actionName"),
  ENTITY_REPRESENTATION("entityRepresentation"),
  ACTION_CONTENT("actionContent"),
  NOTIFICATION_CONTENT("notificationContent");

  private static final String PREFIX = "org.hyperagents.yggdrasil.eventbus.fields.";

  private final String name;

  MessageFields(final String name) {
    this.name = PREFIX + name;
  }

  public String getName() {
    return this.name;
  }
}
