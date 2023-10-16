package org.hyperagents.yggdrasil.cartago;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import cartago.*;
import cartago.events.ArtifactObsEvent;
import cartago.tools.Console;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.PubSubVerticle;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;
import org.hyperagents.yggdrasil.store.RdfStore;
import org.hyperagents.yggdrasil.websub.HttpNotificationVerticle;

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
import org.hyperagents.yggdrasil.websub.NotificationSubscriberRegistry;

public class CartagoVerticle extends AbstractVerticle {
  public static final String BUS_ADDRESS = "org.hyperagents.yggdrasil.eventbus.cartago";

  public static final String CREATE_WORKSPACE = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".createWorkspace";
  public static final String CREATE_SUB_WORKSPACE = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".createSubWorkspace";
  public static final String JOIN_WORKSPACE = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".joinWorkspace";
  public static final String LEAVE_WORKSPACE = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".leaveWorkspace";

  public static final String FOCUS = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".focus";
  public static final String CREATE_ARTIFACT = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".instantiateArtifact";
  public static final String CREATE_BODY = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".createAgentBody";
  public static final String DO_ACTION = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".performAction";

  public static final String AGENT_ID = "org.hyperagents.yggdrasil.eventbus.headers.agentID";

  public static final String WORKSPACE_NAME = "org.hyperagents.yggdrasil.eventbus.headers.workspaceName";
  public static final String SUB_WORKSPACE_NAME = "org.hyperagents.yggdrasil.eventbus.headers.subWorkspaceName";
  public static final String ARTIFACT_NAME = "org.hyperagents.yggdrasil.eventbus.headers.artifactName";
  public static final String ACTION_NAME = "org.hyperagents.yggdrasil.eventbus.headers.actionName";

  private static final Logger LOGGER = LoggerFactory.getLogger(CartagoVerticle.class.getName());
  //private Map<String, CartagoContext> agentContexts;

  private Map<String, AgentCredential> agentCredentials;

  @Override
  public void start() {
    HypermediaArtifactRegistry registry = HypermediaArtifactRegistry.getInstance();
    registry.addArtifactTemplate("http://example.org/Body", AgentBodyArtifact.class.getCanonicalName());
    registry.addArtifactTemplate("http://example.org/Console", Console.class.getCanonicalName());
    JsonObject knownArtifacts = config().getJsonObject("known-artifacts");
    if (knownArtifacts != null) {
      registry.addArtifactTemplates(knownArtifacts);
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

    //agentContexts = new Hashtable<>();
    agentCredentials = new Hashtable<>();

    EventBus eventBus = vertx.eventBus();
    eventBus.consumer(BUS_ADDRESS, this::handleCartagoRequest);

    try {
      LOGGER.info("Starting CArtAgO node...");
      CartagoEnvironment.getInstance().init();
      CartagoEnvironment.getInstance().installInfrastructureLayer("web");
      CartagoEnvironment.getInstance().startInfrastructureService("web");
      //new CartagoPerceptFetcher().start();
    } catch (Exception exception) {
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
          String workspaceDescription = instantiateWorkspace(workspaceName);
          message.reply(workspaceDescription);
          break;
        case CREATE_SUB_WORKSPACE:
          String subWorkspaceName = message.headers().get(SUB_WORKSPACE_NAME);
          String subWorkspaceDescription = instantiateSubWorkspace(workspaceName, subWorkspaceName);
          message.reply(subWorkspaceDescription);
        case JOIN_WORKSPACE:
          String bodyName = joinWorkspace(agentUri, workspaceName);
          String bodyDescription = HypermediaArtifactRegistry.getInstance().getArtifactDescription(bodyName);
          message.reply(bodyDescription);
          break;
        case LEAVE_WORKSPACE:
          leaveWorkspace(agentUri, workspaceName);
          message.reply("agent left workspace successully");
          break;

        case CREATE_ARTIFACT:
          String artifactName = message.headers().get(ARTIFACT_NAME);

          JsonObject artifactInit = (JsonObject) Json.decodeValue(message.body());
          String artifactClass = HypermediaArtifactRegistry.getInstance()
            .getArtifactTemplate(artifactInit.getString("artifactClass")).orElseThrow(); //TODO: check
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
        case FOCUS:
          System.out.println("start focus");
          artifactName = message.headers().get(ARTIFACT_NAME);
          JsonObject body = (JsonObject) Json.decodeValue(message.body());
          String callbackIri = body.getString("callbackIri");
          System.out.println("agent uri: "+agentUri);
          System.out.println("workspace: "+workspaceName);
          System.out.println("artifactName: "+artifactName);
          focus(agentUri, workspaceName, artifactName, callbackIri);
          message.reply(HttpStatus.SC_OK);
          break;
        case DO_ACTION:
          String artifact = message.headers().get(ARTIFACT_NAME);
          String action = message.headers().get(ACTION_NAME);

          Optional<String> payload = message.body() == null ? Optional.empty()
            : Optional.of(message.body());

          Optional<Object> returnObject = doAction(agentUri, workspaceName, artifact, action, payload);
          if (returnObject.isPresent()) {
            //System.out.println("object returned: "+returnObject.get());
            message.reply(returnObject.get());
          } else {
            message.reply(HttpStatus.SC_OK);
          }
          break;
        default:
          // TODO
          break;
      }
    } catch (NoSuchElementException | CartagoException e) {
      message.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }

  }

  private String instantiateWorkspace(String workspaceName)
    throws CartagoException {
    LOGGER.info("Creating workspace " + workspaceName);
    WorkspaceDescriptor descriptor = CartagoEnvironment.getInstance().getRootWSP().getWorkspace().createWorkspace(workspaceName);
    // TODO: handle env IRIs
    String workspaceId = HypermediaArtifactRegistry.getInstance().getHttpWorkspacesPrefix()
      + workspaceName;
    WorkspaceRegistry.getInstance().registerWorkspace(descriptor, workspaceId);

    ThingDescription td = new ThingDescription.Builder(workspaceName)
      .addThingURI(workspaceId)
      .addSemanticType("http://w3id.org/eve#WorkspaceArtifact")
      .addSemanticType("https://ci.mines-stetienne.fr/hmas/core#Workspace")
      .addAction(new ActionAffordance.Builder("makeArtifact",
        new Form.Builder(workspaceId + "/artifacts/").build())
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
      .addAction(new ActionAffordance.Builder("joinWorkspace",
        new Form.Builder(workspaceId + "/join")
          .setMethodName("PUT")
          .build())
        .addSemanticType("http://example.org/joinWorkspace")
        .build())
      .addAction(new ActionAffordance.Builder("leaveWorkspace",
        new Form.Builder(workspaceId + "/leave")
          .setMethodName("DELETE")
          .build())
        .addSemanticType("http://example.org/leaveWorkspace")
        .build())
      .addAction(new ActionAffordance.Builder("focus", new Form.Builder(workspaceId + "/focus")
        .setMethodName("POST")
        .build())
        .addInputSchema(new ObjectSchema.Builder()
          .addProperty("artifactName", new StringSchema.Builder().build())
          .addProperty("artifactIri", new StringSchema.Builder().build())
          .addProperty("callbackIri", new StringSchema.Builder().build())
          .addRequiredProperties("artifactName", "artifactIri", "callbackIri")
          .build())
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
      .setNamespace("hmas", "https://ci.mines-stetienne.fr/hmas/core#")
      .write();
  }

  private String instantiateSubWorkspace(String workspaceName, String subWorkspaceName)
    throws CartagoException {
    LOGGER.info("Creating workspace " + subWorkspaceName);
    Workspace currentWorkspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName);
    WorkspaceDescriptor descriptor = currentWorkspace.createWorkspace(subWorkspaceName);
    String subWorkspaceId = HypermediaArtifactRegistry.getInstance().getHttpWorkspacesPrefix()
      + subWorkspaceName;
    WorkspaceRegistry.getInstance().registerWorkspace(descriptor, subWorkspaceId);

    ThingDescription td = new ThingDescription.Builder(subWorkspaceName)
      .addThingURI(subWorkspaceId)
      .addSemanticType("http://w3id.org/eve#WorkspaceArtifact")
      .addSemanticType("https://ci.mines-stetienne.fr/hmas/core#Workspace")
      .addAction(new ActionAffordance.Builder("makeArtifact",
        new Form.Builder(subWorkspaceId + "/artifacts/").build())
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
      .addAction(new ActionAffordance.Builder("joinWorkspace",
        new Form.Builder(subWorkspaceId + "/join")
          .setMethodName("PUT")
          .build())
        .addSemanticType("http://example.org/joinWorkspace")
        .build())
      .addAction(new ActionAffordance.Builder("leaveWorkspace",
        new Form.Builder(subWorkspaceId + "/leave")
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
      .setNamespace("hmas", "https://ci.mines-stetienne.fr/hmas/core#")
      .write();
  }

  private String joinWorkspace(String agentUri, String workspaceName) {
    System.out.println(agentUri + "joins workspace " + workspaceName);
    //CartagoContext agentContext = getAgentContext(agentUri);
    try {
      Workspace workspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName);
      //AgentCredential agentCredential = new AgentIdCredential(agentContext.getName());
      AgentCredential agentCredential = getAgentCredential(agentUri);
      ICartagoCallback callback = new InterArtifactCallback(new ReentrantLock());
      workspace.joinWorkspace(agentCredential, callback);
      AgentId agentId = getAgentId(agentCredential, workspace.getId());
      WorkspaceId workspaceId = workspace.getId();
      HypermediaAgentBodyArtifactRegistry registry = HypermediaAgentBodyArtifactRegistry.getInstance();
      boolean b = registry.hasArtifact(agentId, workspaceId);
      String hypermediaBodyName;
      if (b) {
        String bodyName = registry.getArtifact(agentId, workspaceId);
        hypermediaBodyName = registry.getHypermediaName(bodyName);
      } else {
        System.out.println("create body for agent: " + agentUri);
        String bodyName = "body_" + agentUri;
        ArtifactDescriptor descriptor = workspace.getArtifactDescriptor(bodyName);
        ArtifactId bodyId = workspace.getArtifact(bodyName);
        HypermediaInterface hypermediaInterface = HypermediaInterface.getBodyInterface(workspace, descriptor, bodyId, agentUri);
        HypermediaArtifactRegistry.getInstance().register(hypermediaInterface);
        hypermediaBodyName = registry.getHypermediaName(bodyName);
        registry.setArtifact(agentId, workspaceId, bodyName);
        registerArtifactEntity(workspace.getId().getName(), hypermediaBodyName);
      }
      return hypermediaBodyName;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }


  private String joinWorkspace2(String agentUri, String workspaceName) {
    System.out.println(agentUri + "joins workspace " + workspaceName);
    //CartagoContext agentContext = getAgentContext(agentUri);
    try {
      Workspace workspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName);
      AgentCredential agentCredential = getAgentCredential(agentUri);
      ICartagoCallback callback = new InterArtifactCallback(new ReentrantLock());
      workspace.joinWorkspace(agentCredential, HypermediaAgentBodyArtifact.class.getCanonicalName(), callback);
      AgentId agentId = getAgentId(agentCredential, workspace.getId());
      WorkspaceId workspaceId = workspace.getId();
      HypermediaAgentBodyArtifactRegistry registry = HypermediaAgentBodyArtifactRegistry.getInstance();
      boolean b = registry.hasArtifact(agentId, workspaceId);
      String hypermediaBodyName;
      if (b) {
        String bodyName = registry.getArtifact(agentId, workspaceId);
        hypermediaBodyName = registry.getHypermediaName(bodyName);
      } else {
        System.out.println("create body for agent: " + agentUri);
        String bodyName = "body_" + agentUri;
        ArtifactDescriptor descriptor = workspace.getArtifactDescriptor(bodyName);
        ArtifactId bodyId = workspace.getArtifact(bodyName);
        HypermediaInterface hypermediaInterface = HypermediaInterface.getBodyInterface(workspace, descriptor, bodyId, agentUri);
        HypermediaArtifactRegistry.getInstance().register(hypermediaInterface);
        hypermediaBodyName = registry.getHypermediaName(bodyName);
        registry.setArtifact(agentId, workspaceId, bodyName);
        registerArtifactEntity(workspace.getId().getName(), hypermediaBodyName);
      }
      return hypermediaBodyName;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public void focus(String agentUri, String workspaceName, String artifactName, String callbackIri){
    Workspace workspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName);
    AgentId agentId = getAgentId(new AgentIdCredential(agentUri), workspace.getId());
    IEventFilter filter = p -> true;
    ICartagoCallback callback = new NotificationCallback(this.vertx, callbackIri);
    ArtifactId artifactId = workspace.getArtifact(artifactName);
    try {
      String artifactIri = HypermediaArtifactRegistry.getInstance().getArtifactUri(workspaceName, artifactName);
      workspace.focus(agentId, filter, callback, artifactId);
      workspace.getArtifactDescriptor(artifactName).addObserver(agentId, filter, callback);
      System.out.println("artifact IRI: "+ artifactIri);
      System.out.println("callback IRI: "+ callbackIri);
      NotificationSubscriberRegistry.getInstance().addCallbackIRI(artifactIri, callbackIri);
      ArrayList<ArtifactObsProperty> properties = workspace.getArtifactDescriptor(artifactName).getArtifact().readAllProperties();
      if (properties.size()>0){
        for (ArtifactObsProperty p : properties) {
          DeliveryOptions options = new DeliveryOptions()
            .addHeader(HttpEntityHandler.REQUEST_METHOD, HttpNotificationVerticle.ARTIFACT_OBS_PROP)
            .addHeader(HttpEntityHandler.REQUEST_URI, artifactIri);

          vertx.eventBus().send(HttpNotificationVerticle.BUS_ADDRESS, p.toString(),
            options);

          LOGGER.info("message sent to notification verticle");
        }
      }
    } catch (Exception e){
      e.printStackTrace();
    }
  }

  /*public List<ArtifactObsProperty> getObsProperties(String agentUri, String workspaceName, String artifactName){
    Workspace workspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName);
    AgentId agentId = getAgentId(new AgentIdCredential(agentUri), workspace.getId());
    try {
      return workspace.getArtifactInfo(artifactName).getObsProperties(); //TODO: the method getArtifactInfo was made public. Check this.

    } catch (Exception e){
      e.printStackTrace();
    }
    return new ArrayList<>();

  }*/

  private void registerArtifactEntity(String workspaceName, String artifactName) {
    String workspaceUri = HypermediaArtifactRegistry.getInstance().getHttpArtifactsPrefix(workspaceName);
    String artifactDescription = HypermediaArtifactRegistry.getInstance().getArtifactDescription(artifactName);
    DeliveryOptions options = new DeliveryOptions()
      .addHeader("org.hyperagents.yggdrasil.eventbus.headers.requestMethod", RdfStore.CREATE_ENTITY)
      .addHeader("org.hyperagents.yggdrasil.eventbus.headers.requestUri", workspaceUri)
      .addHeader("org.hyperagents.yggdrasil.eventbus.headers.slug", artifactName);
    vertx.eventBus().request(RdfStore.BUS_ADDRESS, artifactDescription, options, result -> {
      if (result.succeeded()) {
        LOGGER.info("artifact stored");
      } else {
        LOGGER.info("artifact could not be stored");
      }
    });
  }

  private void leaveWorkspace(String agentUri, String workspaceName) {
    Workspace workspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName);
    WorkspaceId workspaceId = workspace.getId();
    AgentCredential agentCredential = getAgentCredential(agentUri);
    AgentId agent = getAgentId(agentCredential, workspaceId);
    try {
      String bodyName = HypermediaAgentBodyArtifactRegistry.getInstance().getArtifact(agent, workspaceId);
      ArtifactId bodyId = workspace.getArtifact(bodyName);
      workspace.disposeArtifact(agent, bodyId);
      String hypermediaBodyName = HypermediaAgentBodyArtifactRegistry.getInstance().getHypermediaName(bodyName);
      deleteArtifactEntity(workspaceName, hypermediaBodyName);
      workspace.quitAgent(agent);
    } catch (CartagoException e) {
      e.printStackTrace();
    }
  }

  private void deleteArtifactEntity(String workspaceName, String artifactName) {
    String artifactUri = HypermediaArtifactRegistry.getInstance().getHttpArtifactsPrefix(workspaceName) + artifactName;
    DeliveryOptions options = new DeliveryOptions()
      .addHeader("org.hyperagents.yggdrasil.eventbus.headers.requestMethod", RdfStore.DELETE_ENTITY)
      .addHeader("org.hyperagents.yggdrasil.eventbus.headers.requestUri", artifactUri)
      .addHeader("org.hyperagents.yggdrasil.eventbus.headers.slug", artifactName);
    vertx.eventBus().request(RdfStore.BUS_ADDRESS, "", options, result -> {
      if (result.succeeded()) {
        LOGGER.info("artifact deleted");
      } else {
        LOGGER.info("artifact could not be deleted");
      }
    });

  }


  private void instantiateArtifact(String agentUri, String workspaceName, String artifactClass,
                                   String artifactName, Optional<Object[]> params) throws CartagoException {
    //CartagoContext agentContext = getAgentContext(agentUri);
    try {
      joinWorkspace(agentUri, workspaceName);
      //joinWorkspace(agentContext.getName(), workspaceName);


      LOGGER.info("Creating artifact " + artifactName + " of class: " + artifactClass);
      Workspace workspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName);
      ArtifactId artifactId;
      ArtifactDescriptor descriptor;
      AgentCredential agentCredential = getAgentCredential(agentUri);
      if (params.isPresent()) {
        LOGGER.info("Creating artifact with params...");
        AgentId agentId = getAgentId(agentCredential, workspace.getId());
        artifactId = workspace.makeArtifact(agentId, artifactName, artifactClass, new ArtifactConfig(params.get()));
        descriptor = workspace.getArtifactDescriptor(artifactName);
        LOGGER.info("Done!");
      } else {
        LOGGER.info("Creating artifact...");
        AgentId agentId = getAgentId(agentCredential, workspace.getId());
        artifactId = workspace.makeArtifact(agentId, artifactName, artifactClass, new ArtifactConfig());
        descriptor = workspace.getArtifactDescriptor(artifactName);
        LOGGER.info("Done!");
      }
      HypermediaArtifactRegistry registry = HypermediaArtifactRegistry.getInstance();
      if (registry.hasInterfaceConstructor(artifactClass)) {
        HypermediaInterfaceConstructor constructor = registry.getInterfaceConstructor(artifactClass);
        HypermediaInterface hypermediaInterface = constructor.createHypermediaInterface(workspace, descriptor, artifactId);
        registry.register(hypermediaInterface);

      }


    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  private Optional<Object> doAction(String agentUri, String workspaceName, String artifactName, String action,
                                    Optional<String> payload) throws CartagoException {
    AgentCredential agentCredential = getAgentCredential(agentUri);
    Optional<Object> returnObject = Optional.empty();
    //CartagoContext agentContext = getAgentContext(agentUri);
    WorkspaceId workspaceId = WorkspaceRegistry.getInstance().getWorkspaceId(workspaceName);
    joinWorkspace(agentUri, workspaceName);
    Op operation;
    boolean b = false;
    if (payload.isPresent()) {
      Object[] params = CartagoDataBundle.fromJson(payload.get());
      HypermediaArtifactRegistry registry = HypermediaArtifactRegistry.getInstance();
      if (registry.hasHypermediaInterface(artifactName)) {
        HypermediaInterface hypermediaInterface = HypermediaArtifactRegistry.getInstance().getHypermediaInterface(artifactName);
        params = hypermediaInterface.convert(action, params);
      }
      if (registry.hasFeedbackParam(artifactName, action)) {
        b = true;
        List<Object> paramList = new ArrayList<>(Arrays.asList(params));
        OpFeedbackParam<Object> feedbackParam = new OpFeedbackParam<>();
        paramList.add(feedbackParam);
        params = paramList.toArray();
      }

      operation = new Op(action, params);
    } else {
      operation = new Op(action);
    }
    LOGGER.info("Performing action " + action + " on artifact " + artifactName
      + " with params: " + Arrays.asList(operation.getParamValues()));

    Workspace workspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName);
    AgentId agentId = getAgentId(agentCredential, workspaceId);
    //ICartagoCallback callback = new EventManagerCallback(new EventManager());
    ICartagoCallback callback = new NotificationCallback(this.vertx, agentUri); //TODO: check agentUri as callbackIri
    IAlignmentTest alignmentTest = new BasicAlignmentTest(new HashMap<>());
    workspace.execOp(100, agentId, callback, artifactName, operation, 1000, alignmentTest);
    if (b) {
      Object[] params = operation.getParamValues();
      if (params.length > 0) {
        OpFeedbackParam<Object> fParam = (OpFeedbackParam<Object>) params[params.length - 1];
        double maxTime = System.currentTimeMillis() + 1000;
        while (fParam.get() == null && System.currentTimeMillis() < maxTime) {
        }
        Object o = fParam.get();
        LOGGER.info("result: " + o);
        if (o != null) {
          System.out.println("return object is present");
          if (HypermediaArtifactRegistry.getInstance().hasFeedbackResponseConverter(artifactName, action)) {
            ResponseConverter responseConverter = HypermediaArtifactRegistry.getInstance().getFeedbackResponseConverter(artifactName, action);
            o = responseConverter.convert(o);
          }
          returnObject = Optional.of(o.toString());
        } else {
          System.out.println("return object is null");
        }
      }
    }
    ArtifactId artifactId = workspace.getArtifact(artifactName);
    //agentContext.joinWorkspace(workspaceName);
    /*ActionFeedback actionFeedback = agentContext.doActionAsync(artifactId, operation, 1000);
    Object[] returnParams = actionFeedback.getOp().getParamValues();
    int n = returnParams.length;
    if (b && n>0){
      OpFeedbackParam<Object> feedbackParam = (OpFeedbackParam<Object>) returnParams[n-1];
      Object o = feedbackParam.get();
      System.out.println("result: "+o);
    }*/
    DeliveryOptions options = new DeliveryOptions().addHeader(PubSubVerticle.REQUEST_METHOD, PubSubVerticle.PUBLISH)
      .addHeader(PubSubVerticle.TOPIC_NAME, "cartago action")
      .addHeader(PubSubVerticle.SENDER, CartagoVerticle.BUS_ADDRESS);
    JsonObject jsonMessage = new JsonObject();
    jsonMessage.put("actionName", action);
    //ArtifactId artifactId = workspace.getArtifact(artifactName);
    jsonMessage.put("artifactType", artifactId.getArtifactType());
    jsonMessage.put("workspace", workspaceName);
    System.out.println("message to send: " + jsonMessage);
    vertx.eventBus().send(PubSubVerticle.BUS_ADDRESS, jsonMessage.encode(), options);
    System.out.println("message sent");
    return returnObject;
  }

  /*private CartagoContext getAgentContext(String agentUri) {
    printAgentContexts();
    System.out.println("agent uri: " + agentUri);
    if (!agentContexts.containsKey(agentUri)) {
      CartagoContext context = new CartagoContext(new AgentIdCredential(agentUri));
      System.out.println("context name: " + context.getName());
      //agentContexts.put(agentUri, context); //TODO: reuse agentContexts
      printAgentContexts();

      return context;
    } else {
      printAgentContexts();
      return agentContexts.get(agentUri);
    }
  }*/

  /*private CartagoContext getAgentContext(String agentUri) {
    AgentContextRegistry agentContextRegistry = AgentContextRegistry.getInstance();
    agentContextRegistry.printRegistry();
    if (agentContextRegistry.hasContext(agentUri)){
      System.out.println("has context");
      return agentContextRegistry.getContext(agentUri);
    } else {
      System.out.println("create context");
      agentContextRegistry.createNewContext(agentUri);
      agentContextRegistry.printRegistry();
      return agentContextRegistry.getContext(agentUri);
    }
  }*/

  /*private void printAgentContexts() {
    System.out.println("print agent contexts");
    for (String key : agentContexts.keySet()) {
      System.out.println("agent: " + key + " has context: " + agentContexts.get(key).getName());
    }
  }*/

  /*private CartagoContext getAgentContext(String agentUri){;
    printAgentContexts();
    Predicate<CartagoContext> p = (Predicate<CartagoContext>) cartagoContext -> {
      boolean b = false;
      if (cartagoContext.getName().equals(agentUri) ){
        b = true;
      }
      return b;
    };
    if ( agentContexts.size()>0 && agentContexts.stream().anyMatch(p)){
      Optional<CartagoContext> opContext = agentContexts.stream().filter(p).findFirst();
      if (opContext.isPresent()){
        printAgentContexts();
        return opContext.get();
      } }else {
        System.out.println("in else");
        CartagoContext context = new CartagoContext(new AgentIdCredential(agentUri));
        agentContexts.add(context);
        printAgentContexts();
        return context;
      }
    return null;
  }

  private void printAgentContexts(){
    System.out.println("agent contexts");
    for (CartagoContext c: agentContexts){
      System.out.println(c.getName());
    }
  }*/

  /*private AgentId getAgentId(CartagoContext agentContext, WorkspaceId workspaceId) {
    AgentIdCredential credential = new AgentIdCredential(agentContext.getName());
    return new AgentId(credential.getId(), credential.getGlobalId(), 0, credential.getRoleName(), workspaceId);
  }*/

  private AgentId getAgentId(AgentCredential credential, WorkspaceId workspaceId) {
    return new AgentId(credential.getId(), credential.getGlobalId(), 0, credential.getRoleName(), workspaceId);
  }

  private AgentCredential getAgentCredential(String agentUri) {
    printAgentCredentials();
    if (agentCredentials.containsKey(agentUri)) {
      printAgentCredentials();
      return agentCredentials.get(agentUri);
    } else {
      agentCredentials.put(agentUri, new AgentIdCredential(agentUri));
      printAgentCredentials();
      return agentCredentials.get(agentUri);
    }
  }

  private void printAgentCredentials() {
    System.out.println("print agent credentials");
    for (String key : agentCredentials.keySet()) {
      System.out.println("agent: " + key + " has credential: " + agentCredentials.get(key).getId());
    }
  }

  class CartagoPerceptFetcher extends Thread {

    public void run(){
        while (true) {
          try {
            //System.out.println("new loop");
            List<String> workspaces = WorkspaceRegistry.getInstance().getAllWorkspaces();
            for (String w : workspaces) {
              Workspace workspace = WorkspaceRegistry.getInstance().getWorkspace(w);
              String[] artifacts = workspace.getArtifactList();
              for (String a : artifacts) {
                ArtifactId artifactId = workspace.getArtifact(a);
                String artifactIri = HypermediaArtifactRegistry.getInstance()
                  .getHttpArtifactsPrefix(workspace.getId().getName()) + a;
                ArtifactObsProperty[] added = workspace.getArtifactDescriptor(a).getArtifact().getPropsAdded();
                ArtifactObsProperty[] removed = workspace.getArtifactDescriptor(a).getArtifact().getPropsRemoved();
                ArtifactObsProperty[] changed = workspace.getArtifactDescriptor(a).getArtifact().getPropsChanged();
                if (added.length>0 || removed.length>0 || changed.length>0){
                  System.out.println("prepare notification");
                  ArtifactObsEvent e = new ArtifactObsEvent(0, artifactId, null, changed, added, removed);
                  DeliveryOptions options = new DeliveryOptions()
                    .addHeader(HttpEntityHandler.REQUEST_METHOD, HttpNotificationVerticle.ARTIFACT_OBS_PROP)
                    .addHeader(HttpEntityHandler.REQUEST_URI, artifactIri);
                  vertx.eventBus().send(HttpNotificationVerticle.BUS_ADDRESS, e.toString(), options);
                  workspace.getArtifactDescriptor(a).getArtifact().commitChanges();
                }

              }
            }

            sleep(2000); //TODO: change

          } catch (InterruptedException e){
            e.printStackTrace();
          }
        }
    }

    /*public void run(){
      while (true) {
        try {
          List<String> workspaces = WorkspaceRegistry.getInstance().getAllWorkspaces();
          for (String w : workspaces) {
            Workspace workspace = WorkspaceRegistry.getInstance().getWorkspace(w);
            String[] artifacts = workspace.getArtifactList();
            for (String a : artifacts) {
              ArtifactId artifactId = workspace.getArtifact(a);
              String artifactIri = HypermediaArtifactRegistry.getInstance()
                .getHttpArtifactsPrefix(workspace.getId().getName()) + a;
              ArrayList<ArtifactObsProperty> properties = workspace.getArtifactDescriptor(a).getArtifact().readAllProperties(); //TODO: check
                if (properties !=null && properties.size()>0) {
                  //System.out.println("send notifications");
                  for (ArtifactObsProperty p : properties) {
                    DeliveryOptions options = new DeliveryOptions()
                      .addHeader(HttpEntityHandler.REQUEST_METHOD, HttpNotificationVerticle.ARTIFACT_OBS_PROP)
                      .addHeader(HttpEntityHandler.REQUEST_URI, artifactIri);
                    vertx.eventBus().send(HttpNotificationVerticle.BUS_ADDRESS, p.toString(), options);

                  }
                } else {
                    //System.out.println("send no notification");

                }

              }

            }

          sleep(2000); //TODO: change

        } catch (Exception e) {
          System.out.println("exception caught");
          e.printStackTrace();
        }
      }
    }*/

    /*public void run() {
      try {
        while (true) {
          for (AgentCredential credential : agentCredentials.values()) { //TODO: see if correct
            ICartagoListener listener = new ICartagoListener() {
              @Override
              public boolean notifyCartagoEvent(CartagoEvent ev) {
                boolean b = false;
                if (ev instanceof ArtifactObsEvent){
                  b = true;
                }
                return b;
              }
            };
            AgentSession agentSession = new AgentSession(credential, "role", listener); //TODO: check because the constructor was made public
            //CartagoContext context = new CartagoContext(credential);
            //CartagoBasicContext context = new CartagoBasicContext(agentUri);
            //Percept percept = context.fetchPercept();
            CartagoEvent e = agentSession.fetchNextPercept();
            System.out.println("event: "+ e.toString());
            if (e instanceof ArtifactObsEvent) {
              ArtifactObsEvent a = (ArtifactObsEvent) e;
              System.out.println("artifact event: "+ a);
              Percept percept = new Percept(a);
              if (percept != null) {
                LOGGER.info(printPercept(percept) + " for agent " + credential.getId()+ " from artifact "
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
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (CartagoException e) {
        throw new RuntimeException(e);
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

      StringBuilder output = new StringBuilder();
      for (ArtifactObsProperty prop : props) {
        output.append(prop.toString()).append(", id: ").append(prop.getId()).append(", fullId: ").append(prop.getFullId()).append(", annot: ").append(prop.getAnnots());
      }

      return output.toString();
    }*/

    /*private List<String> getArtifacts(CartagoContext context) {
      List<String> artifactNames = new ArrayList<>();
      List<Workspace> workspaces = new ArrayList<>();
      WorkspaceRegistry registry = WorkspaceRegistry.getInstance();
      List<String> workspaceNames = registry.getAllWorkspaces();
      for (String wname : workspaceNames) {
        workspaces.add(registry.getWorkspace(wname));
      }
      for (Workspace workspace : workspaces) {
        String[] artNames = workspace.getArtifactList();
        for (int i = 0; i < artNames.length; i++) {
          ArtifactDescriptor descriptor = workspace.getArtifactDescriptor(artNames[i]);
          List<ArtifactObserver> observers = descriptor.getObservers();
          for (ArtifactObserver observer : observers) {
            if (observer.getAgentId().equals(getAgentId(context, workspace.getId()))) {
              artifactNames.add(artNames[i]);
            }
          }
        }
      }
      return artifactNames;
    }
  }*/

  /*private Object[] createParam(Object... objs) {
    return objs;
  }*/

  /*private String getOpString(Op operation) {
    String s = "";
    s = s + "operation name: " + operation.getName() + "\n";
    Object[] params = operation.getParamValues();
    int n = params.length;
    for (int i = 0; i < n; i++) {
      Object obj = params[i];
      Class c = obj.getClass();
      s = s + "object: " + obj.toString() + ", class: " + c.toString() + "\n";
    }
    return s;
  }*/

  /*private String getParamString(Object[] params) {
    String s = "";
    int n = params.length;
    for (int i = 0; i < n; i++) {
      Object obj = params[i];
      Class c = obj.getClass();
      s = s + "object: " + obj + ", class: " + c.toString() + "\n";
    }
    return s;
  }*/

  }
}



