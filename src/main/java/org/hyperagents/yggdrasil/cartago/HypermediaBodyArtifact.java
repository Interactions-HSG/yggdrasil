package org.hyperagents.yggdrasil.cartago;

import cartago.*;
import cartago.util.agent.CartagoContext;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.security.NoSecurityScheme;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;

import java.util.*;


public class HypermediaBodyArtifact extends Artifact {

  private AgentId agentId;

  private Workspace workspace;

  private EventManager eventManager;

  //From Hypermedia Artifact

  private final Map<String, List<ActionAffordance>> actionAffordances = new HashMap<>();

  private SecurityScheme securityScheme = new NoSecurityScheme();
  private final Model metadata = new LinkedHashModel();

  //End

  public HypermediaBodyArtifact(){
    this.agentId = null;
    this.workspace = null;
    this.eventManager = new EventManager();
  }

  public HypermediaBodyArtifact(AgentId agentId, Workspace workspace){
    this.agentId = agentId;
    this.workspace = workspace;
    this.eventManager = new EventManager();
  }


  public void init(AgentId agentId, Workspace workspace){
    this.agentId = agentId;
    this.workspace = workspace;
    this.eventManager = new EventManager();
  }

  //From HypermediaArtifact

  public String getHypermediaDescription() {
    System.out.println("start get hypermedia description");
    ThingDescription.Builder tdBuilder = new ThingDescription.Builder(getArtifactName())
      .addSecurityScheme(securityScheme)
      .addSemanticType("http://w3id.org/eve#Artifact")
//      .addSemanticType(getSemanticType())
      .addThingURI(getArtifactUri())
      .addGraph(metadata);
System.out.println("builder created");
    for (String actionName : actionAffordances.keySet()) {
      for (ActionAffordance action : actionAffordances.get(actionName)) {
        tdBuilder.addAction(action);
      }
    }
    System.out.println("actions added");

    return new TDGraphWriter(tdBuilder.build())
      .setNamespace("td", "https://www.w3.org/2019/wot/td#")
      .setNamespace("htv", "http://www.w3.org/2011/http#")
      .setNamespace("hctl", "https://www.w3.org/2019/wot/hypermedia#")
      .setNamespace("wotsec", "https://www.w3.org/2019/wot/security#")
      .setNamespace("dct", "http://purl.org/dc/terms/")
      .setNamespace("js", "https://www.w3.org/2019/wot/json-schema#")
      .setNamespace("eve", "http://w3id.org/eve#")
      .write();
  }

  public ArtifactId getArtifactId() {
    return this.getId();
  }

  protected String getArtifactName() {
    return this.getId().getName();
  }

  protected String getArtifactUri() {
    String wkspName = getId().getWorkspaceId().getName();

    return HypermediaArtifactRegistry.getInstance().getHttpArtifactsPrefix(wkspName)
      + getArtifactName();
  }

  protected final void registerActionAffordance(String actionClass, String actionName,
                                                String relativeUri) {
    registerActionAffordance(actionClass, actionName, relativeUri, null);
  }

  protected final void registerActionAffordance(String actionClass, String actionName,
                                                String relativeUri, DataSchema inputSchema) {
    registerActionAffordance(actionClass, actionName, "POST", relativeUri, inputSchema);
  }

  protected final void registerActionAffordance(String actionClass, String actionName,
                                                String methodName, String relativeUri, DataSchema inputSchema) {
    ActionAffordance.Builder actionBuilder = new ActionAffordance.Builder(
      new Form.Builder(getArtifactUri() + relativeUri)
        .setMethodName(methodName)
        .build())
      .addSemanticType(actionClass)
      .addName(actionName)
      .addTitle(actionName);

    if (inputSchema != null) {
      actionBuilder.addInputSchema(inputSchema);
    }

    registerActionAffordance(actionName, actionBuilder.build());
  }

  protected final void registerActionAffordance(String actionName, ActionAffordance action) {
    List<ActionAffordance> actions = actionAffordances.getOrDefault(actionName, new ArrayList<>());

    actions.add(action);
    actionAffordances.put(actionName, actions);
  }

  protected final void setSecurityScheme(SecurityScheme scheme) {
    this.securityScheme = scheme;
  }

  protected final void addMetadata(Model model) {
    this.metadata.addAll(model);
  }

  Map<String, List<ActionAffordance>> getActionAffordances() {
    return actionAffordances;
  }

  private String getSemanticType() {
    Optional<String> semType = HypermediaArtifactRegistry.getInstance().getArtifactSemanticType(
      this.getClass().getCanonicalName());

    if (semType.isPresent()) {
      return semType.get();
    }

    throw new RuntimeException("Artifact was not registered!");
  }

  //TODO: fix setup operations
  @Override
  protected void setupOperations() throws CartagoException {
    super.setupOperations();
    registerInteractionAffordances();
    HypermediaAgentBodyArtifactRegistry.getInstance().register(this);
  }


  @OPERATION
  public void focus(String artifactName){
    ArtifactId artifactId = workspace.getArtifact(artifactName);
    focus(artifactId);
  }


  @OPERATION
  public void focus(ArtifactId artifactId){
    focus(artifactId, null);
  }

  @OPERATION
  public void focus(ArtifactId artifactId, IEventFilter filter ){
    boolean b = workspace.isArtifactPresent(artifactId.getName());
    if (b){
      ICartagoCallback callback = new EventManagerCallback(eventManager);
      try {
        workspace.focus(agentId, filter, callback, artifactId);
      } catch(CartagoException e){
        e.printStackTrace();
      }
    }
}

  @OPERATION
  public void stopFocus(String artifactName){
    ArtifactId artifactId = workspace.getArtifact(artifactName);
    stopFocus(artifactId);
  }

  @OPERATION
  public void stopFocus(ArtifactId artifactId){
    AgentId userId = agentId;
    ICartagoCallback callback = new EventManagerCallback(eventManager); //new InterArtifactCallback(new ReentrantLock());
    try {
      workspace.stopFocus(userId, callback, artifactId);
    } catch(CartagoException e){
      e.printStackTrace();
    }
  }

  @OPERATION
  public void nextEvent(OpFeedbackParam<String> param){
  CartagoEventWrapper ev = eventManager.remove();
  String event = ev.toString();
  param.set(event);
  }

  @INTERNAL_OPERATION
  public void sendEvents(){
    if (eventManager.size()>0){
      CartagoEventWrapper eventWrapper = eventManager.remove();
      sendMessage(eventWrapper.toString());
    }
  }

  private void sendMessage(String message){

  }


  @GUARD
  private boolean isAgent(){
    boolean b = false;
    AgentId agent = this.getCurrentOpAgentId();
    if (agentId.equals(agent)){
      b = true;
    }
    return b;

  }

  protected void registerInteractionAffordances() {
  }





}
