package org.hyperagents.yggdrasil.utils;

import io.vertx.core.shareddata.Shareable;

public interface HttpInterfaceConfig extends Shareable {
  String getHost();

  int getPort();

  String getBaseUri();

  String getWorkspacesUri();

  String getWorkspaceUri(String workspaceName);

  String getArtifactsUri(String workspaceName);

  String getArtifactUri(String workspaceName, String artifactName);

  String getAgentBodiesUri(String workspaceName);

  String getAgentBodyUri(String workspaceName, String agentName);

  String getAgentUri(String agentName);
}
