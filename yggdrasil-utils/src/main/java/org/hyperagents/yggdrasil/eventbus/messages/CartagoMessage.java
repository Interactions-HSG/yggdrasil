package org.hyperagents.yggdrasil.eventbus.messages;



/**
 * Classifies all messages related to Cartago.
 */
public sealed interface CartagoMessage {
  String workspaceName();

  /**
   * Message used to create a Workspace.
   *
   * @param workspaceName The name of the workspace to be created.
   */
  record CreateWorkspace(String workspaceName) implements CartagoMessage {
  }

  /**
   * A record representing a request to create a subworkspace in a specified workspace in Cartago.
   *
   * <p>This record is used if an agent wants to create a subworkspace within a specific workspace.
   * The workspace is identified by its workspaceName and the subworkspace by its subWorkspaceName.
   *
   * @param workspaceName    The name of the workspace where the subworkspace will be created.
   * @param subWorkspaceName The name of the subworkspace to be created.
   */
  record CreateSubWorkspace(String workspaceName,
                            String subWorkspaceName) implements CartagoMessage {
  }

  /**
   * A record representing a request to join a workspace in Cartago.
   *
   * <p>This record is used when an agent wants to join a specific workspace.
   * The agent is identified by its agentId and the workspace by its workspaceName.
   *
   * @param agentId       The unique identifier of the agent that wants to join the workspace.
   * @param workspaceName The name of the workspace the agent wants to join.
   */
  record JoinWorkspace(String agentId,String hint, String workspaceName) implements CartagoMessage {
  }

  /**
   * A record representing a request to leave a workspace in Cartago.
   *
   * <p>This record is used when an agent wants to leave a specific workspace.
   * The agent is identified by its agentId and the workspace by its workspaceName.
   *
   * @param agentId       The unique identifier of the agent that wants to leave the workspace.
   * @param workspaceName The name of the workspace the agent wants to leave.
   */
  record LeaveWorkspace(String agentId, String workspaceName) implements CartagoMessage {
  }

  /**
   * A record representing a request to focus on a specific artifact in a workspace in Cartago.
   *
   * <p>This record is used when an agent wants to focus on a specific artifact within a specific
   * workspace.
   * The agent is identified by its agentId, the workspace by its workspaceName, and the artifact
   * by its artifactName.
   *
   * @param agentId       The unique identifier of the agent that wants to focus on the artifact.
   * @param workspaceName The name of the workspace where the artifact is located.
   * @param artifactName  The name of the artifact the agent wants to focus on.
   */
  record Focus(
      String agentId,
      String workspaceName,
      String artifactName
  ) implements CartagoMessage {
  }

  /**
   * A record representing a request to create an artifact in a workspace in Cartago.
   *
   * <p>This record is used when an agent wants to create a specific artifact within a specific
   * workspace.
   * The agent is identified by its agentId, the workspace by its workspaceName, and the artifact
   * by its artifactName.
   * The representation parameter is a string that represents the state of the artifact.
   *
   * @param agentId        The unique identifier of the agent that wants to create the artifact.
   * @param workspaceName  The name of the workspace where the artifact will be created.
   * @param artifactName   The name of the artifact to be created.
   * @param representation The string representation of the artifact's state.
   */
  record CreateArtifact(
      String agentId,
      String workspaceName,
      String artifactName,
      String representation
  ) implements CartagoMessage {
  }

  /**
   * A record representing a request to perform an action on an artifact in a workspace in Cartago.
   *
   * <p>This record is used when an agent wants to perform a specific action on a
   * specific artifact within a specific workspace.
   * The agent is identified by its agentId, the workspace by its workspaceName,
   * the artifact by its artifactName, and the action by its actionName.
   * The content parameter is an optional string that may contain additional information for the
   * action.
   *
   * @param agentId       The unique identifier of the agent that wants to perform the action.
   * @param workspaceName The name of the workspace where the artifact is located.
   * @param artifactName  The name of the artifact on which the action will be performed.
   * @param actionName    The name of the action to be performed.
   * @param storeResponse The string representation of the response of the action.
   * @param context       The routing context of the request.
   */
  record DoAction(
      String agentId,
      String workspaceName,
      String artifactName,
      String actionName,
      String storeResponse,
      String context
  ) implements CartagoMessage {
  }
}
