package org.hyperagents.yggdrasil.messages;

import java.util.Optional;

public sealed interface CartagoMessage {

  record CreateWorkspace(String agentId, String envName, String workspaceName, String representation) implements CartagoMessage {}

  record CreateArtifact(String agentId, String workspaceName, String artifactName, String representation)
    implements CartagoMessage {}

  record DoAction(String agentId, String workspaceName, String artifactName, String actionName, Optional<String> message)
    implements CartagoMessage {}
}
