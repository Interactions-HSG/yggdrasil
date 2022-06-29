package org.hyperagents.yggdrasil.cartago;

import java.util.*;

import cartago.*;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * A singleton used to manage CArtAgO artifacts. An equivalent implementation can be obtained with
 * local maps in Vert.x. Can be refactored using async shared maps to run over a cluster.
 */
public class HypermediaArtifactRegistry {
  private static HypermediaArtifactRegistry registry;

  private String httpPrefix = "http://localhost:8080";

  // Maps an artifact type IRI to the canonical names of the corresponding CArtAgO artifact class
  // E.g.: "https://ci.mines-stetienne.fr/kg/ontology#PhantomX_3D" ->
  // "org.hyperagents.yggdrasil.cartago.artifacts.PhantomX3D"
  private final Map<String, String> artifactSemanticTypes;

  // Maps the Cname of a CArtAgO artifact to a semantic description of the artifact's HTTP interface
  // exposed by Yggdrasil
  private final Map<String, String> artifactTemplateDescriptions;

  // Maps an HTTP request to an action name. The HTTP request is currently identified by
  // [HTTP_Method] + [HTTP_Target_URI].
  private final Map<String, String> artifactActionRouter;

  // Maps the IRI of an artifact to an API key to be used for that artifact
  private final Map<String, String> artifactAPIKeys;

  private final Map<WorkspaceId, Set<AgentId>> bodyArtifacts;

  private final Map<String, HypermediaInterface> interfaceMap;

  private final Map<String, String> artifactNames;

  private final Map<ImmutablePair<AgentId, WorkspaceId>, String> agentArtifacts;

  private final Map<String, String> hypermediaNames;

  private int n;

  private HypermediaArtifactRegistry() {
    artifactSemanticTypes = new Hashtable<>();
    artifactTemplateDescriptions = new Hashtable<>();
    artifactActionRouter = new Hashtable<>();
    artifactAPIKeys = new Hashtable<>();
    bodyArtifacts = new Hashtable<>();
    interfaceMap = new Hashtable<>();
    artifactNames = new Hashtable<>();
    agentArtifacts = new Hashtable<>();
    hypermediaNames = new Hashtable<>();
    n = 1;
  }

  public static synchronized HypermediaArtifactRegistry getInstance() {
    if (registry == null) {
        registry = new HypermediaArtifactRegistry();
    }

    return registry;
  }

  public void register(HypermediaArtifact artifact) {
    String artifactTemplate = artifact.getArtifactId().getName();
    artifactTemplateDescriptions.put(artifactTemplate, artifact.getHypermediaDescription());

    Map<String, List<ActionAffordance>> actions = artifact.getActionAffordances();

    for (String actionName : actions.keySet()) {
      for (ActionAffordance action : actions.get(actionName)) {
        Optional<Form> form = action.getFirstForm();

        form.ifPresent(value -> {
          if (value.getMethodName().isPresent()) {
            artifactActionRouter.put(value.getMethodName().get() + value.getTarget(), actionName);
          }
        });
      }
    }
  }

  public void register(HypermediaInterface hypermediaInterface) {
    String artifactTemplate = hypermediaInterface.getHypermediaArtifactName();
    artifactTemplateDescriptions.put(artifactTemplate, hypermediaInterface.getHypermediaDescription());

    Map<String, List<ActionAffordance>> actions = hypermediaInterface.getActions();

    for (String actionName : actions.keySet()) {
      for (ActionAffordance action : actions.get(actionName)) {
        Optional<Form> form = action.getFirstForm();

        form.ifPresent(value -> {
          if (value.getMethodName().isPresent()) {
            artifactActionRouter.put(value.getMethodName().get() + value.getTarget(), actionName);
          }
        });
      }
    }
    String artifactName = hypermediaInterface.getActualArtifactName();
    this.interfaceMap.put(artifactName, hypermediaInterface);
    this.artifactNames.put(artifactTemplate, artifactName);
  }

  public void registerName(String bodyName, String hypermediaName){
    hypermediaNames.put(bodyName, hypermediaName);
  }

  public String getHypermediaName(String bodyName){
    return hypermediaNames.get(bodyName);
  }

 /* public void registerBodyArtifact(HypermediaBodyArtifact bodyArtifact){
    //register(bodyArtifact);
    WorkspaceId workspaceId = bodyArtifact.getArtifactId().getWorkspaceId();
    AgentId agentId = bodyArtifact.getArtifactId().getCreatorId();
    if (bodyArtifacts.containsKey(workspaceId)){
      Set<AgentId> agentIds = bodyArtifacts.get(workspaceId);
      agentIds.add(agentId);
      bodyArtifacts.replace(workspaceId, agentIds);
    }
    else {
      Set<AgentId> agentIds = new HashSet<>();
      agentIds.add(agentId);
      bodyArtifacts.put(workspaceId, agentIds);
    }
  }*/

  /*public void registerBodyArtifact(WorkspaceId workspaceId, AgentId agentId){
    if (bodyArtifacts.containsKey(workspaceId)){
      Set<AgentId> agentIds = bodyArtifacts.get(workspaceId);
      agentIds.add(agentId);
      bodyArtifacts.replace(workspaceId, agentIds);
    }
    else {
      Set<AgentId> agentIds = new HashSet<>();
      agentIds.add(agentId);
      bodyArtifacts.put(workspaceId, agentIds);
    }

  }*/

  public void addArtifactTemplate(String key, String value){
    artifactSemanticTypes.put(key, value);
  }



  public void addArtifactTemplates(JsonObject artifactTemplates) {
    if (artifactTemplates != null) {
      artifactTemplates.forEach(entry ->
          artifactSemanticTypes.put(entry.getKey(), (String) entry.getValue()));
    }
  }

  public Set<String> getArtifactTemplates() {
    return artifactSemanticTypes.keySet();
  }

  public Optional<String> getArtifactSemanticType(String artifactTemplate) {
    for (String artifactType : artifactSemanticTypes.keySet()) {
      if (artifactSemanticTypes.get(artifactType).compareTo(artifactTemplate) == 0) {
        return Optional.of(artifactType);
      }
    }

    return Optional.empty();
  }

  public Optional<String> getArtifactTemplate(String artifactClass) {
    String artifactTemplate = artifactSemanticTypes.get(artifactClass);
    return artifactTemplate == null ? Optional.empty() : Optional.of(artifactTemplate);
  }

  public String getArtifactDescription(String artifactName) {
    return artifactTemplateDescriptions.get(artifactName);
  }

  public String getActionName(String method, String requestURI) {
    return artifactActionRouter.get(method + requestURI);
  }

  public void setAPIKeyForArtifact(String artifactId, String apiKey) {
    artifactAPIKeys.put(artifactId, apiKey);
  }

  public String getAPIKeyForArtifact(String artifactId) {
    return artifactAPIKeys.get(artifactId);
  }

  public void setHttpPrefix(String prefix) {
    this.httpPrefix = prefix;
  }

  public String getHttpPrefix() {
    return this.httpPrefix;
  }

  public String getHttpWorkspacesPrefix(){return getHttpPrefix() + "/workspaces/"; }



  public String getHttpArtifactsPrefix(String wkspName){
    return getHttpWorkspacesPrefix() + wkspName +"/artifacts/";
  }



  public Workspace getWorkspaceFromName(String wkspName){
    Workspace mainWorkspace = CartagoEnvironment.getInstance().getRootWSP().getWorkspace();
    Workspace currentWorkspace = mainWorkspace;
    List<Workspace> workspaces = getAllSubWorkspaces(mainWorkspace);
    for (Workspace workspace: workspaces){
      if (workspace.getId().getName()==wkspName || workspace.getId().getFullName()==wkspName){
        currentWorkspace = workspace;
      }
    }
    return currentWorkspace;
  }

  private List<Workspace> getAllSubWorkspaces(Workspace mainWorkspace){
    List<Workspace> workspaces = new ArrayList<>();
    workspaces.add(mainWorkspace);
    Collection<WorkspaceDescriptor> descriptors = mainWorkspace.getChildWSPs();
    if (descriptors.size()>0){
      for (WorkspaceDescriptor descriptor: descriptors){
        Workspace workspace = descriptor.getWorkspace();
        workspaces.addAll(getAllSubWorkspaces(workspace));
      }
    }
    return workspaces;
  }

  public boolean hasHypermediaAgentBody(AgentId agentId, WorkspaceId workspaceId){
    boolean b = false;
    if (bodyArtifacts.containsKey(workspaceId)){
      Set<AgentId> agentIds = bodyArtifacts.get(workspaceId);
      if (agentIds.contains(agentId)){
        b = true;
      }
    }
    return b;
  }

  public boolean hasHypermediaInterface(String artifactName){
    return this.interfaceMap.containsKey(artifactName);
  }

  public HypermediaInterface getHypermediaInterface(String artifactName){
    return this.interfaceMap.get(artifactName);
  }

  public boolean hasOtherName(String hypermediaArtifactName){
    if (artifactNames.containsKey(hypermediaArtifactName)){
      return true;
    } else {
      return false;
    }
  }

  public String getArtifactWithHypermediaInterfaces(){
    Set<String> artifactSet = interfaceMap.keySet();
    return artifactSet.toString();
  }

  public String getActualName(String hypermediaArtifactName){
    return artifactNames.get(hypermediaArtifactName);
  }

  public void setArtifact(AgentId agentId, WorkspaceId workspaceId, String bodyName){
    ImmutablePair<AgentId, WorkspaceId> pair = new ImmutablePair(agentId, workspaceId);
    this.agentArtifacts.put(pair, bodyName);
  }

  public String getArtifact(AgentId agentId, WorkspaceId workspaceId){
    ImmutablePair<AgentId, WorkspaceId> pair = new ImmutablePair(agentId, workspaceId);
    return this.agentArtifacts.get(pair);

  }

  public boolean hasArtifact(AgentId agentId, WorkspaceId workspaceId){
    ImmutablePair<AgentId, WorkspaceId> pair = new ImmutablePair(agentId, workspaceId);
    boolean b = this.agentArtifacts.containsKey(pair);
    return b;

  }

  public String getName(){
    String s ="hypermedia_body_"+n;
    this.n = n +1;
    return s;
  }
}
