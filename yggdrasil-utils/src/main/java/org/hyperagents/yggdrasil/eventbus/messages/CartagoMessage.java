package org.hyperagents.yggdrasil.eventbus.messages;

import java.util.Optional;

/**
 * TODO: Javadoc.
 */
public sealed interface CartagoMessage {
  String workspaceName();

  /**
   * TODO: Javadoc.
   */
  record CreateWorkspace(String workspaceName) implements CartagoMessage {}

  /**
   * TODO: Javadoc.
   */
  record CreateSubWorkspace(String workspaceName, String subWorkspaceName)
      implements CartagoMessage {}

  /**
   * TODO: Javadoc.
   */
  record JoinWorkspace(String agentId, String workspaceName) implements CartagoMessage {}

  /**
   * TODO: Javadoc.
   */
  record LeaveWorkspace(String agentId, String workspaceName) implements CartagoMessage {}

  /**
   * TODO: Javadoc.
   */
  record Focus(
      String agentId,
      String workspaceName,
      String artifactName
  ) implements CartagoMessage {}

  /**
   * TODO: Javadoc.
   */
  record CreateArtifact(
      String agentId,
      String workspaceName,
      String artifactName,
      String representation
  ) implements CartagoMessage {}

  /**
   * TODO: Javadoc.
   */
  record DoAction(
      String agentId,
      String workspaceName,
      String artifactName,
      String actionName,
      Optional<String> content
  ) implements CartagoMessage {}
}
