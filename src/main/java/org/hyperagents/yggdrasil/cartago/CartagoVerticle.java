package org.hyperagents.yggdrasil.cartago;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;

import cartago.AgentIdCredential;
import cartago.ArtifactId;
import cartago.CartagoContext;
import cartago.CartagoException;
import cartago.CartagoService;
import cartago.Op;
import cartago.WorkspaceId;
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
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
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
  public static final String ENV_NAME = "org.hyperagents.yggdrasil.eventbus.headers.envName";
  public static final String WORKSPACE_NAME = "org.hyperagents.yggdrasil.eventbus.headers.workspaceName";
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
      String host = httpConfig.getString("virtual-host", "localhost");
      Integer port = httpConfig.getInteger("virtual-host-port");
      
      if (port == null) {
        HypermediaArtifactRegistry.getInstance().setHttpPrefix("http://" + host);
      } else {
        HypermediaArtifactRegistry.getInstance().setHttpPrefix("http://" + host + ":" + port);
      }
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
    String workspaceName = message.headers().get(WORKSPACE_NAME);
    
    try {
      switch (requestMethod) {
        case CREATE_WORKSPACE:
          String envName = message.headers().get(ENV_NAME);
          String workspaceDescription = instatiateWorkspace(agentUri, envName, workspaceName);
          message.reply(workspaceDescription);
          break;
        case CREATE_ARTIFACT:
          String artifactName = message.headers().get(ARTIFACT_NAME);
          
          JsonObject artifactInit = (JsonObject) Json.decodeValue(message.body());
          String artifactClass = HypermediaArtifactRegistry.getInstance()
              .getArtifactTemplate(artifactInit.getString("artifactClass")).get();
          JsonArray initParams = artifactInit.getJsonArray("initParams");
          
          Optional<Object[]> params = Optional.empty();
          if (initParams != null) {
            params = Optional.of(initParams.getList().toArray());
          }
          
          instantiateArtifact(agentUri, workspaceName, artifactClass, artifactName, params);
          
          String artifactDescription = HypermediaArtifactRegistry.getInstance()
              .getArtifactDescription(artifactName);
          
          message.reply(artifactDescription);
          break;
        case DO_ACTION:
          String artifact = message.headers().get(ARTIFACT_NAME);
          String action = message.headers().get(ACTION_NAME);
          
          Optional<String> payload = message.body() == null ? Optional.empty() 
              : Optional.of(message.body());
          
          doAction(agentUri, workspaceName, artifact, action, payload);
          message.reply(HttpStatus.SC_OK);
          break;
        default:
          // TODO
          break;
      }
    } catch (NoSuchElementException | CartagoException e) {
      message.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
    
  }
  
  private String instatiateWorkspace(String agentUri, String envName, String workspaceName) 
      throws ActionFailedException, CartagoException {
    CartagoContext agentContext = getAgentContext(agentUri);
    LOGGER.info("Creating workspace " + workspaceName);
    agentContext.doAction(new Op("createWorkspace", workspaceName));
    
    // TODO: handle env IRIs
    String workspaceId = HypermediaArtifactRegistry.getInstance().getHttpWorkspacesPrefix(envName) 
        + workspaceName;
    
    ThingDescription td = new ThingDescription.Builder(workspaceName)
        .addThingURI(workspaceId)
        .addSemanticType("http://w3id.org/eve#WorkspaceArtifact")
        .addAction(new ActionAffordance.Builder(new Form.Builder(workspaceId + "/artifacts/").build())
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
    
    return new TDGraphWriter(td)
        .setNamespace("td", "https://www.w3.org/2019/wot/td#")
        .setNamespace("htv", "http://www.w3.org/2011/http#")
        .setNamespace("hctl", "https://www.w3.org/2019/wot/hypermedia#")
        .setNamespace("wotsec", "https://www.w3.org/2019/wot/security#")
        .setNamespace("dct", "http://purl.org/dc/terms/")
        .setNamespace("js", "https://www.w3.org/2019/wot/json-schema#")
        .setNamespace("eve", "http://w3id.org/eve#")
        .write();
  }
  
  private void instantiateArtifact(String agentUri, String workspaceName, String artifactClass, 
      String artifactName, Optional<Object[]> params) throws CartagoException {
    CartagoContext agentContext = getAgentContext(agentUri); 
    
    WorkspaceId wkspId = agentContext.joinWorkspace(workspaceName);
    
    LOGGER.info("Creating artifact " + artifactName + " of class: " + artifactClass);
    
    if (params.isPresent()) {
      agentContext.makeArtifact(wkspId, artifactName, artifactClass, params.get());
    } else {
      agentContext.makeArtifact(wkspId, artifactName, artifactClass);
    }
  }
  
  private void doAction(String agentUri, String workspaceName, String artifactName, String action, 
      Optional<String> payload) throws CartagoException {
    CartagoContext agentContext = getAgentContext(agentUri);
    
    WorkspaceId workspaceId = agentContext.joinWorkspace(workspaceName);
    
    Op operation;
    
    if (payload.isPresent()) {
      Object[] params = CartagoDataBundle.fromJson(payload.get());
      operation = new Op(action, params);
    } else {
      operation = new Op(action);
    }
    
    LOGGER.info("Performing action " + action + " on artifact " + artifactName 
        + " with params: " + Arrays.asList(operation.getParamValues()));
    
    ArtifactId artifactId = agentContext.lookupArtifact(workspaceId, artifactName);
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
