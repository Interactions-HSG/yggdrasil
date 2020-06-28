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
import cartago.util.agent.ActionFailedException;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class CartagoVerticle extends AbstractVerticle {
  public static final String BUS_ADDRESS = "org.hyperagents.yggdrasil.eventbus.cartago";

  public static final String CREATE_WORKSPACE = "org.hyperagents.yggdrasil.eventbus.headers.methods"
      + ".createWorkspace";
  public static final String CREATE_ARTIFACT = "org.hyperagents.yggdrasil.eventbus.headers.methods"
      + ".instantiateArtifact";
  public static final String DO_ACTION = "org.hyperagents.yggdrasil.eventbus.headers.methods"
      + ".performAction";
  
  public static final String AGENT_ID = "org.hyperagents.yggdrasil.eventbus.headers.agentID";
  public static final String ARTIFACT_CLASS = "org.hyperagents.yggdrasil.eventbus.headers.artifactClass";
  public static final String ARTIFACT_NAME = "org.hyperagents.yggdrasil.eventbus.headers.artifactName";
  public static final String ACTION_NAME = "org.hyperagents.yggdrasil.eventbus.headers.actionName";
  
  private static final Logger LOGGER = LoggerFactory.getLogger(CartagoVerticle.class.getName());
  
  private Map<String, CartagoContext> agentContexts;
  
  @Override
  public void start() {
    JsonObject knownArtifacts = config().getJsonObject("known-artifacts");
    if (knownArtifacts != null) {
      HypermediaArtifactRegistry.getInstance().addArtifactTemplates(knownArtifacts);
    }
    
    JsonObject httpConfig = config().getJsonObject("http-config");
    if (httpConfig != null) {
      int port = httpConfig.getInteger("port", 8080);
      String host = httpConfig.getString("host", "localhost");
      
      HypermediaArtifactRegistry.getInstance().setHttpPrefix("http://" + host + ":" + port);
    }
    
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

    try {
    switch (requestMethod) {
      case CREATE_WORKSPACE:
        String workspaceName = message.headers().get(HttpEntityHandler.ENTITY_URI_HINT);
        String workspaceDescription = instatiateWorkspace(agentUri, workspaceName);
        message.reply(workspaceDescription);
        break;
      case CREATE_ARTIFACT:
        String artifactClass = message.headers().get(ARTIFACT_CLASS);
        String artifactName = message.headers().get(HttpEntityHandler.ENTITY_URI_HINT);
        
//        Object[] params = new Object[] {
//          message.headers().get("robotUri"),
//          message.headers().get("apiKey"),
//          Integer.valueOf(message.headers().get("xr")),
//          Integer.valueOf(message.headers().get("yr"))
//        };
        
        instantiateArtifact(agentUri, artifactClass, artifactName, Optional.empty());
        
        String artifactDescription = HypermediaArtifactRegistry.getInstance()
            .getArtifactDescription(artifactName);
        
        message.reply(artifactDescription);
        break;
      case DO_ACTION:
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
    } catch (CartagoException e) {
      message.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
    
  }
  
  private String instatiateWorkspace(String agentUri, String workspaceName) throws ActionFailedException, 
      CartagoException {
    CartagoContext agentContext = getAgentContext(agentUri);
    LOGGER.info("Creating workspace " + workspaceName);
    agentContext.doAction(new Op("createWorkspace", workspaceName));
    
    ThingDescription td = new ThingDescription.Builder(workspaceName)
        .addSemanticType("http://w3id.org/eve#WorkspaceArtifact")
        .addAction(new ActionAffordance.Builder(new Form.Builder(HypermediaArtifactRegistry
                .getInstance().getHttpPrefix() + "/artifacts/").build())
            .addSemanticType("http://w3id.org/eve#MakeArtifact")
            .addInputSchema(new ObjectSchema.Builder()
                .addProperty("artifactClass", new StringSchema.Builder()
                    .addSemanticType("http://w3id.org/eve#ArtifactClass")
                    .addEnum(HypermediaArtifactRegistry.getInstance().getArtifactTemplates())
                    .build())
                .addProperty("initParams", new ArraySchema.Builder()
                    .build())
                .addRequiredProperties("artifactClass")
                .build())
            .build())
        .build();
    
    return TDGraphWriter.write(td);
  }
  
  private void instantiateArtifact(String agentUri, String artifactClass, String artifactName, 
      Optional<Object[]> params) throws CartagoException {
    CartagoContext agentContext = getAgentContext(agentUri); 
    
    LOGGER.info("Creating artifact " + artifactName + " of class: " + artifactClass);
    
    if (params.isPresent()) {
      agentContext.makeArtifact(artifactName, artifactClass, params.get());
    } else {
      agentContext.makeArtifact(artifactName, artifactClass);
    }
  }
  
  private void doAction(String agentUri, String artifactName, String action, Optional<String> payload) 
      throws CartagoException {
    CartagoContext agentContext = getAgentContext(agentUri);
    
    Op operation;
    
    if (payload.isPresent()) {
      Object[] params = CartagoDataBundle.fromJson(payload.get());
      operation = new Op(action, params);
    } else {
      operation = new Op(action);
    }
    
    LOGGER.info("Performing action " + action + " on artifact " + artifactName 
        + " with params: " + Arrays.asList(operation.getParamValues()));
    
    ArtifactId artifactId = agentContext.lookupArtifact(artifactName);
    agentContext.doAction(artifactId, operation);
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
