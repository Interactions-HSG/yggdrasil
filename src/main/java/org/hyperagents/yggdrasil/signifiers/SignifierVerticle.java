package org.hyperagents.yggdrasil.signifiers;

import cartago.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;

import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;

public class SignifierVerticle extends AbstractVerticle {

  public static final String BUS_ADDRESS = "org.hyperagents.yggdrasil.eventbus.signifier";

  public static final String AGENT_ID = "org.hyperagents.yggdrasil.eventbus.headers.agentID";

  public static final String GET_SIGNIFIER = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".getSignifier";

  public static final String SIGNIFIER_NAME = "org.hyperagents.yggdrasil.eventbus.headers.signifierName";


  @Override
  public void start(){
    EventBus eventBus = vertx.eventBus();
    eventBus.consumer(BUS_ADDRESS, this::handleSignifierRequest);
  }

  private void handleSignifierRequest(Message<String> message){
    String agentUri = message.headers().get(AGENT_ID);
    if (agentUri == null) {
      message.fail(HttpStatus.SC_BAD_REQUEST, "Agent WebID is missing.");
      return;
    }

    String requestMethod = message.headers().get(HttpEntityHandler.REQUEST_METHOD);
    try {
      switch (requestMethod){
        case GET_SIGNIFIER:
          String signifier = message.headers().get(SIGNIFIER_NAME);

          String signifierContent = getSignifier(agentUri, signifier);
          message.reply(signifierContent);
          break;
        default:
          break;
      }
    }
    catch(Exception e){
      e.printStackTrace();
    }

  }

  private String getSignifier(String agentUri, String signifier){
    SignifierRegistry registry = SignifierRegistry.getInstance();
    return registry.getSignifier(agentUri, signifier);
  }


}
