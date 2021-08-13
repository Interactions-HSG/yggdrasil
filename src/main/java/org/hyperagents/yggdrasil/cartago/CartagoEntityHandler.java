package org.hyperagents.yggdrasil.cartago;

import org.hyperagents.yggdrasil.http.HttpEntityHandler;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class CartagoEntityHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(CartagoEntityHandler.class.getName());
  private Vertx vertx;

  public CartagoEntityHandler(Vertx vertx) {
    this.vertx = vertx;
  }

  /**
   * Creates a workspace on the local CArtAgO node.
   *
   * @param agentId the identifier of the agent creating the workspace (e.g., a URI)
   * @param workspaceName the preferred name for the workspace to be created
   * @param representation representation of the workspace to be created
   * @param result a promise with the result of the create workspace operation
   */
  public void createWorkspace(String agentId, String envName, String workspaceName,
      String representation, Promise<String> result) {
    DeliveryOptions options = getDeliveryOptions(agentId, workspaceName)
        .addHeader(HttpEntityHandler.REQUEST_METHOD, CartagoVerticle.CREATE_WORKSPACE)
        .addHeader(CartagoVerticle.ENV_NAME, envName);

    sendCartagoMessage(representation, options, result);
  }

  /**
   * Creates an artifact within an existing workspace on the local CArtAgO node.
   *
   * @param agentId agentId the identifier of the agent creating the workspace (e.g., a URI)
   * @param workspaceName the name of the workspace
   * @param artifactName the preferred name for the artifact to be created
   * @param representation representation of the artifact to be created
   * @param result a promise with the result of the create artifact operation
   */
  public void createArtifact(String agentId, String workspaceName, String artifactName,
      String representation, Promise<String> result) {
    LOGGER.info("Collecting delivery options, workspace: " + workspaceName);
  LOGGER.info("name :"+artifactName);
    DeliveryOptions options = getDeliveryOptions(agentId, workspaceName)
        .addHeader(HttpEntityHandler.REQUEST_METHOD, CartagoVerticle.CREATE_ARTIFACT)
        .addHeader(CartagoVerticle.ARTIFACT_NAME, artifactName);
    LOGGER.info("artifact name: "+artifactName);
    LOGGER.info("Sending cartago message to create artifact...");

    sendCartagoMessage(representation, options, result);
  }

  private DeliveryOptions getDeliveryOptions(String agentId, String workspaceName) {
    return new DeliveryOptions().addHeader(CartagoVerticle.AGENT_ID, agentId)
        .addHeader(CartagoVerticle.WORKSPACE_NAME, workspaceName);
  }

  private void sendCartagoMessage(String message, DeliveryOptions options, Promise<String> result) {
    vertx.eventBus().request(CartagoVerticle.BUS_ADDRESS, message, options,
        response -> {
          if (response.succeeded()) {
            String workspaceDescription = (String) response.result().body();
            result.complete(workspaceDescription);
            LOGGER.info("CArtAgO workspace created: " + workspaceDescription);
          } else {
            result.fail("CArtAgO operation has failed.");
          }
        });
  }

}
