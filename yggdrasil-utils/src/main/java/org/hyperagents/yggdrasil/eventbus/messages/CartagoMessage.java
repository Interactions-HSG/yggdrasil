package org.hyperagents.yggdrasil.eventbus.messages;

import java.util.Optional;

public sealed interface CartagoMessage {
  String workspaceName();

  record CreateWorkspace(String workspaceName) implements CartagoMessage {}

  record CreateSubWorkspace(String workspaceName, String subWorkspaceName)
    implements CartagoMessage {}

  record JoinWorkspace(String agentId, String workspaceName) implements CartagoMessage {}

  record LeaveWorkspace(String agentId, String workspaceName) implements CartagoMessage {}

  record Focus(
    String agentId,
    String workspaceName,
    String artifactName,
    String callbackIri
  ) implements CartagoMessage {}

  record CreateArtifact(
      String agentId,
      String workspaceName,
      String artifactName,
      String representation
  ) implements CartagoMessage {}

  record DoAction(
      String agentId,
      String workspaceName,
      String artifactName,
      String actionName,
      Optional<String> content
  ) implements CartagoMessage {}
}
