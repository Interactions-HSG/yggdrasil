package org.hyperagents.yggdrasil.messages;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;

import java.util.Optional;

public interface CartagoMessagebox {

  /**
   * Creates a workspace on the local CArtAgO node.
   *
   * @param agentId the identifier of the agent creating the workspace (e.g., a URI)
   * @param workspaceName the preferred name for the workspace to be created
   * @param representation representation of the workspace to be created
   * @param result a promise with the result of the create workspace operation
   */
  void createWorkspace(
    String agentId,
    String envName,
    String workspaceName,
    String representation,
    Handler<AsyncResult<Message<String>>> handler
  );

  /**
   * Creates an artifact within an existing workspace on the local CArtAgO node.
   *
   * @param agentId agentId the identifier of the agent creating the workspace (e.g., a URI)
   * @param workspaceName the name of the workspace
   * @param artifactName the preferred name for the artifact to be created
   * @param representation representation of the artifact to be created
   * @param result a promise with the result of the create artifact operation
   */
  void createArtifact(
    String agentId,
    String workspaceName,
    String artifactName,
    String representation,
    Handler<AsyncResult<Message<String>>> handler
  );

  void doAction(
    String agentId,
    String workspaceName,
    String artifactName,
    String actionName,
    Optional<String> message,
    Handler<AsyncResult<Message<Void>>> handler
  );
}
