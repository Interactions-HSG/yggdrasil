package org.hyperagents.yggdrasil.jason;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import org.hyperagents.yggdrasil.cartago.CartagoVerticle;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;

public class EnvironmentHandler {

  Vertx vertx;

  public EnvironmentHandler(Vertx vertx){
    this.vertx = vertx;
  }

  public void createWorkspace(String agentName, String workspaceName, Promise<String> result){
    DeliveryOptions options = new DeliveryOptions()
      .addHeader(HttpEntityHandler.REQUEST_METHOD, CartagoVerticle.CREATE_WORKSPACE)
      .addHeader(CartagoVerticle.AGENT_ID, agentName)
      .addHeader(CartagoVerticle.WORKSPACE_NAME, workspaceName);
    sendEnvironmentMessage("", options, result );
  }

  private void sendEnvironmentMessage(String message, DeliveryOptions options, Promise<String> result) {
    vertx.eventBus().request(CartagoVerticle.BUS_ADDRESS, message, options,
      response -> {
        if (response.succeeded()) {
          String workspaceDescription = (String) response.result().body();
          result.complete(workspaceDescription);
        } else {
          result.fail("CArtAgO operation has failed.");
        }
      });
  }
}
