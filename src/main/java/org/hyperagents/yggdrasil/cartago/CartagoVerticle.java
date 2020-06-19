package org.hyperagents.yggdrasil.cartago;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.core.EventBusMessage;
import org.hyperagents.yggdrasil.core.EventBusRegistry;
import org.hyperagents.yggdrasil.core.HypermediaArtifactRegistry;

import com.google.gson.Gson;

import cartago.AgentIdCredential;
import cartago.ArtifactId;
import cartago.CartagoContext;
import cartago.CartagoException;
import cartago.CartagoService;
import cartago.Op;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class CartagoVerticle extends AbstractVerticle {
  public static final String AGENT_ID = "headers.agentID";
  public static final String ARTIFACT_CLASS = "headers.artifactClass";
  public static final String ARTIFACT_NAME = "headers.artifactName";
  public static final String ACTION_NAME = "headers.actionName";
  
  private static final Logger LOGGER = LoggerFactory.getLogger(CartagoVerticle.class.getName());

  private Map<String, CartagoContext> agentContexts;
  
  @Override
  public void start() {
    
    agentContexts = new HashMap<String, CartagoContext>();
    
    EventBus eventBus = vertx.eventBus();
    eventBus.consumer(EventBusRegistry.CARTAGO_BUS_ADDRESS, this::handleCartagoRequest);
    
    try {
      LOGGER.info("Starting CArtAgO node...");
      CartagoService.startNode();
    } catch (CartagoException e) {
      LOGGER.error(e.getMessage());
    }
  }
  
  private void handleCartagoRequest(Message<String> message) {
    EventBusMessage request = (new Gson()).fromJson(message.body().toString(), EventBusMessage.class);
    
    String agentUri = message.headers().get(AGENT_ID);
    
    if (agentUri == null) {
      message.fail(HttpStatus.SC_BAD_REQUEST, "Agent WebID is missing.");
      return;
    }
    
    switch (request.getMessageType()) {
      case INSTANTIATE_ARTIFACT:
        String artifactClass = message.headers().get(ARTIFACT_CLASS);
        String artifactName = request.getHeader(EventBusMessage.Headers.ENTITY_IRI_HINT).get();
        
        instantiateArtifact(agentUri, artifactClass, artifactName);
        
        String artifactDescription = HypermediaArtifactRegistry.getInstance()
            .getArtifactDescription(artifactName);
        
        LOGGER.info("TD: " + artifactDescription);
        
        message.reply(artifactDescription);
        break;
      case DO_ACTION:
        String artifact = message.headers().get(ARTIFACT_NAME);
        String action = message.headers().get(ACTION_NAME);
        doAction(agentUri, artifact, action, request.getPayload());
        message.reply(EventBusMessage.ReplyStatus.SUCCEEDED);
        break;
      default:
        // TODO
        break;
    }
    
  }
  
  private void instantiateArtifact(String agentUri, String artifactClass, String artifactName) {
    CartagoContext agentContext = getAgentContext(agentUri); 
    
    try {
      LOGGER.info("Creating artifact " + artifactName + " of class: " + artifactClass);
      agentContext.makeArtifact(artifactName, artifactClass);
    } catch (CartagoException e) {
      e.printStackTrace();
    }
  }
  
  private void doAction(String agentUri, String artifactName, String action, 
      Optional<String> payload) {
    CartagoContext agentContext = getAgentContext(agentUri);
    
    Op operation;
    
    if (payload.isPresent()) {
      Object[] params = CartagoDataBundle.fromJson(payload.get());
      operation = new Op(action, params);
    } else {
      operation = new Op(action);
    }
    
    try {
      LOGGER.info("Performing action " + action + " on artifact " + artifactName 
          + " with params: " + Arrays.asList(operation.getParamValues()));
      
      ArtifactId artifactId = agentContext.lookupArtifact(artifactName);
      agentContext.doAction(artifactId, operation);
    } catch (CartagoException e) {
      e.printStackTrace();
    }
  }
  
  private CartagoContext getAgentContext(String agentUri) {
    if (!agentContexts.containsKey(agentUri)) {
      CartagoContext context = new CartagoContext(new AgentIdCredential(agentUri));
      agentContexts.put(agentUri, context);
      
      return context;
    } else {
      return agentContexts.get(agentUri);
    }
  }
}
