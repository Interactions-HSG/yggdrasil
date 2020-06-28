package org.hyperagents.yggdrasil.http;

import org.hyperagents.yggdrasil.cartago.CartagoVerticle;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class WorkspaceHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceHandler.class.getName());
  private Vertx vertx;
  
  public WorkspaceHandler(Vertx vertx) {
    this.vertx = vertx;
  }
  
  /**
   * Validates the create workspace request and sends a message to the CArtAgO verticle.
   * 
   * @param agentId the identifier of the agent creating the workspace (e.g., a URI) 
   * @param workspaceName the preferred name for the workspace to be created
   * @param representation a representation of the workspace
   */
  public void createWorkspace(String agentId, String workspaceName, String representation, 
      Promise<String> result) {
    DeliveryOptions options = new DeliveryOptions()
        .addHeader(CartagoVerticle.AGENT_ID, agentId)
        .addHeader(HttpEntityHandler.REQUEST_METHOD, CartagoVerticle.CREATE_WORKSPACE)
        .addHeader(HttpEntityHandler.ENTITY_URI_HINT, workspaceName);
    
    vertx.eventBus().request(CartagoVerticle.BUS_ADDRESS, representation, options,
        response -> {
          if (response.succeeded()) {
            String workspaceDescription = (String) response.result().body();
            result.complete(workspaceDescription);
            LOGGER.info("CArtAgO workspace created: " + workspaceDescription);
          } else {
            result.fail("CArtAgO workspace creation has failed.");
          }
        });
  }
  
}
