package org.hyperagents.yggdrasil.utils;

import java.util.Optional;

public interface HttpInterfaceConfig {
  String getHost();

  int getPort();

  int getCartagoPort();

  String getBaseUri();

  Optional<String> getWebSubHubUri();

  String getWorkspacesUri();

  String getWorkspaceUri(String workspaceName);

  String getArtifactsUri(String workspaceName);

  String getArtifactUri(String workspaceName, String artifactName);
}
