package org.hyperagents.yggdrasil.cartago;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import cartago.*;
import cartago.events.ArtifactObsEvent;
import cartago.tools.Console;
import cartago.util.agent.ActionFeedback;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.PubSubVerticle;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;
import org.hyperagents.yggdrasil.store.RdfStore;
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
  public static final String CREATE_SUB_WORKSPACE = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".createSubWorkspace";
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

  public static final String WORKSPACE_NAME = "org.hyperagents.yggdrasil.eventbus.headers.workspaceName";
  public static final String SUB_WORKSPACE_NAME = "org.hyperagents.yggdrasil.eventbus.headers.subWorkspaceName";
  public static final String ARTIFACT_NAME = "org.hyperagents.yggdrasil.eventbus.headers.artifactName";
  public static final String ACTION_NAME = "org.hyperagents.yggdrasil.eventbus.headers.actionName";

  private static final Logger LOGGER = LoggerFactory.getLogger(CartagoVerticle.class.getName());

  private Map<String, CartagoContext> agentContexts;

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
          String workspaceDescription = instantiateWorkspace(agentUri, workspaceName);
          System.out.println("workspace description: "+workspaceDescription);
          message.reply(workspaceDescription);
          break;
        case CREATE_SUB_WORKSPACE:
          String subWorkspaceName = message.headers().get(SUB_WORKSPACE_NAME);
          String subWorkspaceDescription = instantiateSubWorkspace(agentUri, workspaceName, subWorkspaceName);
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
          System.out.println("Create artifact");
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

          Optional<Object> returnObject = doAction(agentUri, workspaceName, artifact, action, payload);
          if (returnObject.isPresent()){
            System.out.println("object returned: "+returnObject.get());
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

  private String instantiateWorkspace(String agentUri, String workspaceName)
      throws ActionFailedException, CartagoException {
    CartagoContext agentContext = getAgentContext(agentUri);
    LOGGER.info("Creating workspace " + workspaceName);
    WorkspaceDescriptor descriptor = CartagoEnvironment.getInstance().getRootWSP().getWorkspace().createWorkspace(workspaceName);
    // TODO: handle env IRIs
    String workspaceId = HypermediaArtifactRegistry.getInstance().getHttpWorkspacesPrefix()
        + workspaceName;
    WorkspaceRegistry.getInstance().registerWorkspace(descriptor, workspaceId);
    Workspace workspace = descriptor.getWorkspace();

    ThingDescription td = new ThingDescription.Builder(workspaceName)
        .addThingURI(workspaceId)
        .addSemanticType("http://w3id.org/eve#WorkspaceArtifact")
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
          new Form.Builder(workspaceId+"/join")
          .setMethodName("PUT")
          .build())
          .addSemanticType("http://example.org/joinWorkspace")
          .build())
      .addAction(new ActionAffordance.Builder("leaveWorkspace",
        new Form.Builder(workspaceId+"/leave")
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

  private String instantiateSubWorkspace(String agentUri, String workspaceName, String subWorkspaceName)
    throws ActionFailedException, CartagoException {
    CartagoContext agentContext = getAgentContext(agentUri);
    LOGGER.info("Creating workspace " + subWorkspaceName);
    Workspace currentWorkspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName);
    WorkspaceDescriptor descriptor = currentWorkspace.createWorkspace(subWorkspaceName);
    String workspaceId = HypermediaArtifactRegistry.getInstance().getHttpWorkspacesPrefix()
      + workspaceName;
    String subWorkspaceId = HypermediaArtifactRegistry.getInstance().getHttpWorkspacesPrefix()
      + subWorkspaceName;
    WorkspaceRegistry.getInstance().registerWorkspace(descriptor, subWorkspaceId);

    ThingDescription td = new ThingDescription.Builder(subWorkspaceName)
      .addThingURI(subWorkspaceId)
      .addSemanticType("http://w3id.org/eve#WorkspaceArtifact")
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
        new Form.Builder(subWorkspaceId+"/join")
        .setMethodName("PUT")
        .build())
        .addSemanticType("http://example.org/joinWorkspace")
        .build())
      .addAction(new ActionAffordance.Builder("leaveWorkspace",
        new Form.Builder(subWorkspaceId+"/leave")
        .setMethodName("DELETE")
        .build())
        .addSemanticType("http://example.org/leaveWorkspace")
        .build())
      //end new actions
      .build();

    String swts =  new TDGraphWriter(td)
      .setNamespace("td", "https://www.w3.org/2019/wot/td#")
      .setNamespace("htv", "http://www.w3.org/2011/http#")
      .setNamespace("hctl", "https://www.w3.org/2019/wot/hypermedia#")
      .setNamespace("wotsec", "https://www.w3.org/2019/wot/security#")
      .setNamespace("dct", "http://purl.org/dc/terms/")
      .setNamespace("js", "https://www.w3.org/2019/wot/json-schema#")
      .setNamespace("eve", "http://w3id.org/eve#")
      .write();
    System.out.println("subworkspace td string: "+swts);
    return swts;
  }

  private String joinWorkspace(String agentUri, String workspaceName) {
    CartagoContext agentContext = getAgentContext(agentUri);
    try {
      Workspace workspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName);
      AgentCredential agentCredential = new AgentIdCredential(agentContext.getName());
      ICartagoCallback callback = new InterArtifactCallback(new ReentrantLock());
      workspace.joinWorkspace(agentCredential, callback);
      AgentId agentId = getAgentId(agentContext, workspace.getId());
      WorkspaceId workspaceId = workspace.getId();
      HypermediaAgentBodyArtifactRegistry registry = HypermediaAgentBodyArtifactRegistry.getInstance();
      boolean b = registry.hasArtifact(agentId, workspaceId);
      String hypermediaBodyName = "";
      if (b){
        String bodyName = registry.getArtifact(agentId, workspaceId);
        hypermediaBodyName = registry.getHypermediaName(bodyName);
      }
      else {
        String bodyName = "body_" + agentUri;
        ArtifactDescriptor descriptor = workspace.getArtifactDescriptor(bodyName);
        ArtifactId bodyId = workspace.getArtifact(bodyName);
        HypermediaInterface hypermediaInterface = HypermediaInterface.getBodyInterface(workspace, descriptor, bodyId);
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

  private void registerArtifactEntity(String workspaceName, String artifactName){
    String workspaceUri = HypermediaArtifactRegistry.getInstance().getHttpArtifactsPrefix(workspaceName);
    String artifactDescription = HypermediaArtifactRegistry.getInstance().getArtifactDescription(artifactName);
    DeliveryOptions options = new DeliveryOptions()
      .addHeader("org.hyperagents.yggdrasil.eventbus.headers.requestMethod", RdfStore.CREATE_ENTITY)
      .addHeader("org.hyperagents.yggdrasil.eventbus.headers.requestUri", workspaceUri)
      .addHeader("org.hyperagents.yggdrasil.eventbus.headers.slug", artifactName);
    vertx.eventBus().request(RdfStore.BUS_ADDRESS, artifactDescription, options, result -> {
      if (result.succeeded()) {
        System.out.println("artifact stored");
      } else {
        System.out.println("artifact could not be stored");
      }
    });
  }

  private void leaveWorkspace(String agentUri, String workspaceName){
    Workspace workspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName);
    CartagoContext agentContext = getAgentContext(agentUri);
    WorkspaceId workspaceId = workspace.getId();
    AgentId agent = getAgentId(agentContext, workspaceId);
    try {
      String bodyName = HypermediaAgentBodyArtifactRegistry.getInstance().getArtifact(agent, workspaceId);
      System.out.println("body name: "+bodyName);
      ArtifactId bodyId = workspace.getArtifact(bodyName);
      workspace.disposeArtifact(agent, bodyId);
      String hypermediaBodyName = HypermediaAgentBodyArtifactRegistry.getInstance().getHypermediaName(bodyName);
      deleteArtifactEntity(workspaceName, hypermediaBodyName);
      workspace.quitAgent(agent);
    } catch(CartagoException e){
      e.printStackTrace();
    }
  }

  private void deleteArtifactEntity(String workspaceName, String artifactName){
    String artifactUri = HypermediaArtifactRegistry.getInstance().getHttpArtifactsPrefix(workspaceName)+artifactName;
    DeliveryOptions options = new DeliveryOptions()
      .addHeader("org.hyperagents.yggdrasil.eventbus.headers.requestMethod", RdfStore.DELETE_ENTITY)
      .addHeader("org.hyperagents.yggdrasil.eventbus.headers.requestUri", artifactUri)
      .addHeader("org.hyperagents.yggdrasil.eventbus.headers.slug", artifactName);
    vertx.eventBus().request(RdfStore.BUS_ADDRESS, "", options, result -> {
      if (result.succeeded()) {
        System.out.println("artifact deleted");
      } else {
        System.out.println("artifact could not be deleted");
      }
    });

  }


  private void instantiateArtifact(String agentUri, String workspaceName, String artifactClass,
      String artifactName, Optional<Object[]> params) throws CartagoException {
    CartagoContext agentContext = getAgentContext(agentUri);
    try {
      joinWorkspace(agentContext.getName(), workspaceName);
      WorkspaceId wkspId = WorkspaceRegistry.getInstance().getWorkspaceId(workspaceName);
      joinWorkspace(agentContext.getName(), workspaceName);


      LOGGER.info("Creating artifact " + artifactName + " of class: " + artifactClass);
      Workspace workspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName);
      ArtifactId artifactId = null;
      ArtifactDescriptor descriptor = null;
      if (params.isPresent()) {
        LOGGER.info("Creating artifact with params...");
        AgentId agentId = getAgentId(agentContext, workspace.getId());
        artifactId = workspace.makeArtifact(agentId,artifactName, artifactClass, new ArtifactConfig(params.get()));
        descriptor = workspace.getArtifactDescriptor(artifactName);
        LOGGER.info("Done!");
      } else {
        LOGGER.info("Creating artifact...");
        AgentId agentId = getAgentId(agentContext, workspace.getId());
        artifactId = workspace.makeArtifact(agentId,artifactName, artifactClass,new ArtifactConfig());
        descriptor = workspace.getArtifactDescriptor(artifactName);
        LOGGER.info("Done!");
      }
      HypermediaArtifactRegistry registry = HypermediaArtifactRegistry.getInstance();
      if (registry.hasInterfaceConstructor(artifactClass)){
        HypermediaInterfaceConstructor constructor = registry.getInterfaceConstructor(artifactClass);
        HypermediaInterface hypermediaInterface = constructor.createHypermediaInterface(workspace, descriptor, artifactId);
        registry.register(hypermediaInterface);

      }


    } catch(Exception e){
      e.printStackTrace();
    }
  }


  private Optional<Object> doAction(String agentUri, String workspaceName, String artifactName, String action,
      Optional<String> payload) throws CartagoException {
    Optional<Object> returnObject = Optional.empty();
    CartagoContext agentContext = getAgentContext(agentUri);
    WorkspaceId workspaceId = WorkspaceRegistry.getInstance().getWorkspaceId(workspaceName);
    joinWorkspace(agentContext.getName(), workspaceName);
    Op operation;
    boolean b = false;
    if (payload.isPresent()) {
      Object[] params = CartagoDataBundle.fromJson(payload.get());
      HypermediaArtifactRegistry registry = HypermediaArtifactRegistry.getInstance();
      if (registry.hasHypermediaInterface(artifactName)){
        HypermediaInterface hypermediaInterface = HypermediaArtifactRegistry.getInstance().getHypermediaInterface(artifactName);
        params = hypermediaInterface.convert(action, params);
      }
      boolean c = registry.hasFeedbackParam(artifactName, action);
      System.out.println("c: "+c);
      if (registry.hasFeedbackParam(artifactName, action)){
        b = true;
        List<Object> paramList = new ArrayList();
        paramList.addAll(Arrays.asList(params));
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
    AgentId agentId = getAgentId(agentContext, workspaceId);
    //ICartagoCallback callback = new EventManagerCallback(new EventManager());
    ICartagoCallback callback = new NotificationCallback(this.vertx);
    IAlignmentTest alignmentTest = new BasicAlignmentTest(new HashMap<>());
    workspace.execOp(100, agentId, callback, artifactName, operation, 1000, alignmentTest);
    if (b){
      System.out.println("in b loop");
      Object[] params = operation.getParamValues();
      if (params.length>0){
        OpFeedbackParam<Object> fParam = (OpFeedbackParam<Object>) params[params.length-1];
        while (fParam.get()==null){
          System.out.println("wait");
        }
        Object o = fParam.get();
        System.out.println("result: "+o);
        returnObject = Optional.of(o);
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
      .addHeader(PubSubVerticle.TOPIC_NAME, "cartago action");
    JsonObject jsonMessage = new JsonObject();
    jsonMessage.put("actionName", action);
    //ArtifactId artifactId = workspace.getArtifact(artifactName);
    jsonMessage.put("artifactType",artifactId.getArtifactType() );
    jsonMessage.put("workspace", workspaceName);
    vertx.eventBus().send(PubSubVerticle.BUS_ADDRESS, jsonMessage.encode(), options);
    return returnObject;
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

    private List<String> getArtifacts(CartagoContext context){
      List<String> artifactNames = new ArrayList<>();
      List<Workspace> workspaces = new ArrayList<>();
      WorkspaceRegistry registry = WorkspaceRegistry.getInstance();
      List<String> workspaceNames = registry.getAllWorkspaces();
      for (String wname: workspaceNames){
        workspaces.add(registry.getWorkspace(wname));
      }
      for (Workspace workspace: workspaces){
        String[] artNames = workspace.getArtifactList();
        for (int i = 0; i<artNames.length; i++){
          ArtifactDescriptor descriptor = workspace.getArtifactDescriptor(artNames[i]);
          List<ArtifactObserver> observers = descriptor.getObservers();
          for (ArtifactObserver observer: observers){
            if (observer.getAgentId().equals(getAgentId(context, workspace.getId()))){
              artifactNames.add(artNames[i]);
            }
          }
        }
      }
      return artifactNames;
    }
  }

  private Object[] createParam(Object... objs){
    return objs;
  }

  private String getOpString(Op operation){
    String s = "";
    s = s + "operation name: " + operation.getName() +"\n";
    Object[] params = operation.getParamValues();
    int n = params.length;
    for (int i = 0; i<n; i++){
      Object obj = params[i];
      Class c = obj.getClass();
      s = s + "object: " + obj.toString() + ", class: "+ c.toString() + "\n";
    }
    return s;
  }

  private String getParamString(Object[] params) {
    String s = "";
    int n = params.length;
    for (int i = 0; i < n; i++) {
      Object obj = params[i];
      Class c = obj.getClass();
      s = s + "object: " + obj.toString() + ", class: " + c.toString() + "\n";
    }
    return s;
  }
}
