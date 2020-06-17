package org.hyperagents.yggdrasil.cartago;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.core.HypermediaArtifactRegistry;
import org.hyperagents.yggdrasil.core.EventBusMessage;
import org.hyperagents.yggdrasil.core.EventBusRegistry;
import org.hyperagents.yggdrasil.http.HttpTemplateHandler;

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
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


public class CartagoVerticle extends AbstractVerticle {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpTemplateHandler.class.getName());

  private Map<String, CartagoContext> agentContexts;
  
  @Override
  public void start() {
    
    agentContexts = new HashMap<String, CartagoContext>();
    
    EventBus eventBus = vertx.eventBus();
    eventBus.consumer(EventBusRegistry.CARTAGO_BUS_ADDRESS, this::handleCartagoRequest);
    
    try {
      LOGGER.info("Starting CArtAgO node...");
      CartagoService.startNode();
      
      // Creates a new work session for a given agent in the default workspace.
//      CartagoContext agentContext = new CartagoContext(new AgentIdCredential("agent-0"));
//      
//      agentContext.doAction(new Op("println","Hello, world!"));
//      
//      LOGGER.info("Creating artifact: " + Counter.class.getName());
//      
//      agentContext.makeArtifact("c0", Counter.class.getName());
//      
//      LOGGER.info("Focusing on artifact...");
//      
//      agentContext.doAction(new Op("focusWhenAvailable", "c0"));
//      ArtifactId aId = agentContext.lookupArtifact("c0");
//      
//      agentContext.doAction(aId, new Op("inc"));
//      
//      Percept percept = agentContext.fetchPercept();
      
//      while (percept != null) {
//        LOGGER.info("Pecept: " + percept);
//        percept = agentContext.fetchPercept();
//      }
//      
//      LOGGER.info("No percept available");
      
    } catch (CartagoException e) {
      LOGGER.error(e.getMessage());
    } //catch (InterruptedException e) {
//      e.printStackTrace();
//    }
  }
  
  private void handleCartagoRequest(Message<String> message) {
    EventBusMessage request = (new Gson()).fromJson(message.body().toString(), EventBusMessage.class);

    Optional<String> agentUri = request.getHeader(EventBusMessage.Headers.AGENT_WEBID);
    
    if (agentUri.isEmpty()) {
      message.fail(HttpStatus.SC_BAD_REQUEST, "Agent WebID is missing.");
      return;
    }
    
    switch (request.getMessageType()) {
      case INSTANTIATE_ARTIFACT:
        String artifactClass = request.getHeader(EventBusMessage.Headers.ARTIFACT_CLASS).get();
        String artifactName = request.getHeader(EventBusMessage.Headers.ENTITY_IRI_HINT).get();
        
        instantiateArtifact(agentUri.get(), artifactClass, artifactName);
        
        String artifactDescription = HypermediaArtifactRegistry.getInstance()
            .getArtifactDescription(artifactName);
        
        LOGGER.info("TD: " + artifactDescription);
        
        message.reply(artifactDescription);
        break;
      case DO_ACTION:
        JsonObject payload = new JsonObject(request.getPayload().get());
        String name = payload.getString("artifactName");
        String action = payload.getString("action");
        doAction(agentUri.get(), name, action);
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
  
  private void doAction(String agentUri, String artifactName, String action) {
    CartagoContext agentContext = getAgentContext(agentUri);
    
    try {
      LOGGER.info("Performing action " + action + " on artifact " + artifactName);
      
      ArtifactId artifactId = agentContext.lookupArtifact(artifactName);
      
      agentContext.doAction(artifactId, new Op(action));
      
      LOGGER.info("Artifact found, done!");
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
