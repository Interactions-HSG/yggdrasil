package org.hyperagents.yggdrasil.cartago;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import cartago.*;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;
import org.hyperagents.yggdrasil.websub.HttpNotificationVerticle;

import cartago.util.agent.CartagoContext;
import cartago.util.agent.ActionFailedException;
import cartago.util.agent.Percept;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
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
  public static final String JOIN_WORKSPACE = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".joinWorkspace";
  public static final String LEAVE_WORKSPACE = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".leaveWorkspace";
  public static final String CREATE_ARTIFACT = "org.hyperagents.yggdrasil.eventbus.headers.methods"
      + ".instantiateArtifact";
  public static final String CREATE_BODY = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    +".createAgentBody";
  public static final String DO_ACTION = "org.hyperagents.yggdrasil.eventbus.headers.methods"
      + ".performAction";

  public static final String AGENT_ID = "org.hyperagents.yggdrasil.eventbus.headers.agentID";

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

    agentContexts = new Hashtable<>();

    EventBus eventBus = vertx.eventBus();
    eventBus.consumer(BUS_ADDRESS, this::handleCartagoRequest);

    try {
      LOGGER.info("Starting CArtAgO node...");
      CartagoEnvironment.getInstance().init();
      new CartagoPerceptFetcher().start();
    } catch (CartagoException exception){
      exception.printStackTrace();
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
          String workspaceDescription = instantiateWorkspace(agentUri, envName, workspaceName);
          message.reply(workspaceDescription);
          break;
        case JOIN_WORKSPACE:
          envName = message.headers().get(ENV_NAME);
          joinWorkspace(agentUri, envName, workspaceName);
        case LEAVE_WORKSPACE:
          envName = message.headers().get(ENV_NAME);
          leaveWorkspace(agentUri, envName, workspaceName);
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
        case CREATE_BODY:
          artifactName = message.headers().get(ARTIFACT_NAME);
          instantiateHypermediaAgentBody(agentUri, artifactName, workspaceName);
          artifactDescription = HypermediaAgentBodyArtifactRegistry.getInstance()
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

  private String instantiateWorkspace(String agentUri, String envName, String workspaceName)
      throws ActionFailedException, CartagoException {
    CartagoContext agentContext = getAgentContext(agentUri);
    LOGGER.info("Creating workspace " + workspaceName);
    WorkspaceDescriptor descriptor = CartagoEnvironment.getInstance().getRootWSP().getWorkspace().createWorkspace(workspaceName);
    WorkspaceRegistry.getInstance().registerWorkspace(descriptor);
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
                .addProperty("artifactName", new StringSchema.Builder()
                    .addSemanticType("http://w3id.org/eve#ArtifactName")
                    .addEnum(HypermediaArtifactRegistry.getInstance().getArtifactTemplates())
                    .build())
                .addProperty("initParams", new ArraySchema.Builder()
                    .build())
                .addRequiredProperties("artifactClass", "artifactName")
                .build())
            .build())
      //new actions
        .addAction(new ActionAffordance.Builder(new Form.Builder(workspaceId+"/join")
          .setMethodName("PUT")
          .build())
          .addSemanticType("http://example.org/joinWorkspace")
          .build())
      .addAction(new ActionAffordance.Builder(new Form.Builder(workspaceId+"/leave")
        .setMethodName("DELETE")
        .build())
        .addSemanticType("http://example.org/leaveWorkspace")
        .build())
      //end new actions
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

  private String instantiateWorkspace2(String agentUri, String envName, String workspaceName){
    LOGGER.info("Creating workspace " + workspaceName);
    try {
      CartagoEnvironment.getInstance().getRootWSP().getWorkspace().createWorkspace(workspaceName);
    } catch(CartagoException e){
      e.printStackTrace();
    }
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
          .addProperty("artifactName", new StringSchema.Builder()
            .addSemanticType("http://w3id.org/eve#ArtifactName")
            .addEnum(HypermediaArtifactRegistry.getInstance().getArtifactTemplates())
            .build())
          .addProperty("initParams", new ArraySchema.Builder()
            .build())
          .addRequiredProperties("artifactClass", "artifactName")
          .build())
        .build())
      //new actions
      .addAction(new ActionAffordance.Builder(new Form.Builder(workspaceId+"/join")
        .setMethodName("PUT")
        .build())
        .addSemanticType("http://example.org/joinWorkspace")
        .build())
      .addAction(new ActionAffordance.Builder(new Form.Builder(workspaceId+"/leave")
        .setMethodName("DELETE")
        .build())
        .addSemanticType("http://example.org/leaveWorkspace")
        .build())
      //end new actions
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

  private void joinWorkspace(String agentUri, String envName, String workspaceName){
    CartagoContext agentContext = getAgentContext(agentUri);
    try {
      CartagoEnvironment environment = CartagoEnvironment.getInstance();
      Workspace workspace = HypermediaArtifactRegistry.getInstance().getWorkspaceFromName(workspaceName);
      AgentCredential agentCredential = new AgentIdCredential(agentContext.getName());
      ICartagoCallback callback = new InterArtifactCallback(new ReentrantLock());
      workspace.joinWorkspace(agentCredential, callback);
    } catch(CartagoException e){
      e.printStackTrace();
    }
  }

  private void leaveWorkspace(String agentUri, String envName, String workspaceName){
    Workspace workspace = HypermediaArtifactRegistry.getInstance().getWorkspaceFromName(workspaceName);
    CartagoContext agentContext = getAgentContext(agentUri);
    WorkspaceId workspaceId = workspace.getId();
    AgentId agent = getAgentId(agentContext, workspaceId);
    try {
      workspace.quitAgent(agent);
    } catch(CartagoException e){
      e.printStackTrace();
    }
  }


  private void instantiateArtifact(String agentUri, String workspaceName, String artifactClass,
      String artifactName, Optional<Object[]> params) throws CartagoException {
    CartagoContext agentContext = getAgentContext(agentUri);
    try {
      String envName = HypermediaArtifactRegistry.getInstance().getEnvironmentForWorkspace(workspaceName).get();
      joinWorkspace(agentContext.getName(), envName, workspaceName);
      WorkspaceId wkspId = WorkspaceRegistry.getInstance().getWorkspaceId(workspaceName);
      joinWorkspace(agentContext.getName(), envName, workspaceName);


      LOGGER.info("Creating artifact " + artifactName + " of class: " + artifactClass);

      if (params.isPresent()) {
        LOGGER.info("Creating artifact with params...");
        Workspace workspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName);
        AgentId agentId = getAgentId(agentContext, workspace.getId());
        workspace.makeArtifact(agentId,artifactName, artifactClass, new ArtifactConfig(params.get()));
        LOGGER.info("Done!");
      } else {
        LOGGER.info("Creating artifact...");
        Workspace workspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName);
        AgentId agentId = getAgentId(agentContext, workspace.getId());
        workspace.makeArtifact(agentId,artifactName, artifactClass,new ArtifactConfig());
        ArtifactId artId = agentContext.lookupArtifact(wkspId, artifactName);
        artId.getId();
        LOGGER.info("Done!");
      }
    } catch(Exception e){
      e.printStackTrace();
    }
  }

  private void instantiateHypermediaAgentBody(String agentUri, String artifactName, String workspaceName)
    throws CartagoException {
    WorkspaceId workspaceId = WorkspaceRegistry.getInstance().getWorkspaceId(workspaceName);
    CartagoContext agentContext = getAgentContext(agentUri);
    AgentId agentId = getAgentId(agentContext, workspaceId);
    if (!HypermediaArtifactRegistry.getInstance().hasHypermediaAgentBody(agentId, workspaceId)){
      Workspace workspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName);
      String envName = HypermediaArtifactRegistry.getInstance().getEnvironmentForWorkspace(workspaceName).get();
      joinWorkspace(agentContext.getName(), envName, workspaceName);
      String artifactClass = HypermediaAgentBodyArtifact.class.getName();
      workspace.addArtifactFactory(new HypermediaAgentBodyArtifactFactory());
      ArtifactConfig config = new ArtifactConfig(new Object[]{agentId, workspace});
      try {
        workspace.makeArtifact(agentId, artifactName, artifactClass, config);
      } catch (CartagoException e){
        e.printStackTrace();
      }
    }
    LOGGER.info("Done!");
  }


  private void doAction(String agentUri, String workspaceName, String artifactName, String action,
      Optional<String> payload) throws CartagoException {
    CartagoContext agentContext = getAgentContext(agentUri);
    String envName = HypermediaArtifactRegistry.getInstance().getEnvironmentForWorkspace(workspaceName).get();
    WorkspaceId workspaceId = WorkspaceRegistry.getInstance().getWorkspaceId(workspaceName);
    joinWorkspace(agentContext.getName(), envName, workspaceName);
    Op operation;
    if (payload.isPresent()) {
      Object[] params = CartagoDataBundle.fromJson(payload.get());
      operation = new Op(action, params);
    } else {
      operation = new Op(action);
    }
    LOGGER.info("Performing action " + action + " on artifact " + artifactName
        + " with params: " + Arrays.asList(operation.getParamValues()));

    Workspace workspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName);
    AgentId agentId = getAgentId(agentContext, workspaceId);
    workspace.lookupArtifact(agentId, artifactName);
    ICartagoCallback callback = new EventManagerCallback(new EventManager());
    IAlignmentTest alignmentTest = new BasicAlignmentTest(new HashMap<>());
    workspace.execOp(100, agentId, callback, artifactName, operation, 1000, alignmentTest);
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

  private AgentId getAgentId(CartagoContext agentContext, WorkspaceId workspaceId){
    AgentIdCredential credential = new AgentIdCredential(agentContext.getName());
    AgentId agent = new AgentId(credential.getId(), credential.getGlobalId(), 0, credential.getRoleName(), workspaceId);
    return agent;
  }

  class CartagoPerceptFetcher extends Thread {
    public void run() {
      try {
        while (true) {
          for (CartagoContext context : agentContexts.values()) {
//            CartagoContext context = agentContexts.get("agent-0");
            Percept percept = context.fetchPercept();

            if (percept != null) {
              LOGGER.info(printPercept(percept) + " for agent " + context.getName() + " from artifact "
                  + percept.getArtifactSource());

              // Signals don;t have an artifact source
              if (percept.hasSignal() || percept.getPropChanged() == null) {
                continue;
              }

              ArtifactId source = percept.getArtifactSource();

              String artifactIri = HypermediaArtifactRegistry.getInstance()
                  .getHttpArtifactsPrefix(source.getWorkspaceId().getName()) + source.getName();
              LOGGER.info("artifactIri: " + artifactIri + ", precept: " + percept.getPropChanged()[0].toString());

              DeliveryOptions options = new DeliveryOptions()
                  .addHeader(HttpEntityHandler.REQUEST_METHOD, HttpNotificationVerticle.ARTIFACT_OBS_PROP)
                  .addHeader(HttpEntityHandler.REQUEST_URI, artifactIri);

              vertx.eventBus().send(HttpNotificationVerticle.BUS_ADDRESS, percept.getPropChanged()[0].toString(),
                  options);

              LOGGER.info("message sent to notification verticle");
            }
          }

          sleep(100);
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    private String printPercept(Percept percept) {
      String output = "Percept: " + percept.getSignal();

      output += ", [" + printProps(percept.getAddedProperties()) + "]";
      output += ", [" + printProps(percept.getPropChanged()) + "]";
      output += ", [" + printProps(percept.getRemovedProperties()) + "]";

      return output;
    }

    private String printProps(ArtifactObsProperty[] props) {
      if (props == null) {
        return "";
      }

      String output = "";
      for (ArtifactObsProperty prop : props) {
        output += prop.toString() + ", id: " + prop.getId() + ", fullId: " + prop.getFullId()
            + ", annot: " + prop.getAnnots();
      }

      return output;
    }
  }

  private Object[] createParam(Object... objs){
    return objs;
  }
}
