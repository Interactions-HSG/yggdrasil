package org.hyperagents.yggdrasil.moise;

import cartago.AgentBodyArtifact;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import ora4mas.nopl.OrgArt;
import ora4mas.nopl.OrgBoard;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifactRegistry;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;



public class MoiseVerticle extends AbstractVerticle {

  public static final String BUS_ADDRESS = "org.hyperagents.yggdrasil.eventbus.moise";

  public static final String AGENT_ID = "org.hyperagents.yggdrasil.eventbus.headers.agentID";

  public static final String CREATE_GROUP = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".createGroup";

  @Override
  public void start(){
    EventBus eventBus = vertx.eventBus();
    HypermediaArtifactRegistry registry = HypermediaArtifactRegistry.getInstance();
    registry.addArtifactTemplate("http://example.org/OrgBoard", OrgBoard.class.getCanonicalName());
    eventBus.consumer(BUS_ADDRESS, this::handleMoiseRequest);
  }

  private void handleMoiseRequest(Message<String> message) {
    String agentUri = message.headers().get(AGENT_ID);

    if (agentUri == null) {
      message.fail(HttpStatus.SC_BAD_REQUEST, "Agent WebID is missing.");
      return;
    }

    String requestMethod = message.headers().get(HttpEntityHandler.REQUEST_METHOD);
    try {
      switch (requestMethod){
        case CREATE_GROUP:

          System.out.println("group created");

      }

    } catch (Exception e){
      e.printStackTrace();
    }

  }
}
