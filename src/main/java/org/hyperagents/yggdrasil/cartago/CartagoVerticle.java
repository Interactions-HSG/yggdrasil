package org.hyperagents.yggdrasil.cartago;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;

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
  public static final String BUS_ADDRESS = "org.hyperagents.yggdrasil.eventbus.cartago";
  
  public static final String INSTANTIATE_ARTIFACT = "org.hyperagents.yggdrasil.eventbus.headers"
      + ".methods.instantiateArtifact";
  public static final String PERFORM_ACTION = "org.hyperagents.yggdrasil.eventbus.headers"
      + ".methods.performAction";
  
  public static final String AGENT_ID = "org.hyperagents.yggdrasil.eventbus.headers.agentID";
  public static final String ARTIFACT_CLASS = "org.hyperagents.yggdrasil.eventbus.headers.artifactClass";
  public static final String ARTIFACT_NAME = "org.hyperagents.yggdrasil.eventbus.headers.artifactName";
  public static final String ACTION_NAME = "org.hyperagents.yggdrasil.eventbus.headers.actionName";
  
  private static final Logger LOGGER = LoggerFactory.getLogger(CartagoVerticle.class.getName());

  private Map<String, CartagoContext> agentContexts;
  
  @Override
  public void start() {
    HypermediaArtifactRegistry.getInstance().addArtifactTemplates(config());
    
    agentContexts = new HashMap<String, CartagoContext>();
    
    EventBus eventBus = vertx.eventBus();
    eventBus.consumer(BUS_ADDRESS, this::handleCartagoRequest);
    
    try {
      LOGGER.info("Starting CArtAgO node...");
      CartagoService.startNode();
    } catch (CartagoException e) {
      LOGGER.error(e.getMessage());
    }
  }
  
  private void handleCartagoRequest(Message<String> message) {
    String agentUri = message.headers().get(AGENT_ID);
    if (agentUri == null) {
      message.fail(HttpStatus.SC_BAD_REQUEST, "Agent WebID is missing.");
      return;
    }
    
    String requestMethod = message.headers().get(HttpEntityHandler.REQUEST_METHOD);
    
    switch (requestMethod) {
      case INSTANTIATE_ARTIFACT:
        String artifactClass = message.headers().get(ARTIFACT_CLASS);
        String artifactName = message.headers().get(HttpEntityHandler.ENTITY_URI_HINT);
        
        instantiateArtifact(agentUri, artifactClass, artifactName);
        
        String artifactDescription = HypermediaArtifactRegistry.getInstance()
            .getArtifactDescription(artifactName);
        
        message.reply(artifactDescription);
        break;
      case PERFORM_ACTION:
        String artifact = message.headers().get(ARTIFACT_NAME);
        String action = message.headers().get(ACTION_NAME);
        
        Optional<String> payload = message.body() == null ? Optional.empty() 
            : Optional.of(message.body());
        
        doAction(agentUri, artifact, action, payload);
        message.reply(HttpStatus.SC_OK);
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
