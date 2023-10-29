package org.hyperagents.yggdrasil.messages;

public enum MessageHeaders {
  REQUEST_METHOD("requestMethod"),
  REQUEST_URI("requestUri"),
  ENTITY_URI_HINT("slug"),
  CONTENT_TYPE("contentType"),
  AGENT_ID("agentID"),
  ENV_NAME("envName"),
  WORKSPACE_NAME("workspaceName"),
  ARTIFACT_NAME("artifactName"),
  ACTION_NAME("actionName");

  private static final String PREFIX = "org.hyperagents.yggdrasil.eventbus.headers.";

  private final String name;

  MessageHeaders(final String name) {
    this.name = PREFIX + name;
  }

  public String getName() {
    return this.name;
  }
}
