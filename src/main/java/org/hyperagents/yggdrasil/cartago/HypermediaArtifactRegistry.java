package org.hyperagents.yggdrasil.cartago;

import java.util.*;

import cartago.*;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import io.vertx.core.json.JsonObject;

/**
 * A singleton used to manage CArtAgO artifacts. An equivalent implementation can be obtained with
 * local maps in Vert.x. Can be refactored using async shared maps to run over a cluster.
 */
public class HypermediaArtifactRegistry {
  private static HypermediaArtifactRegistry registry;

  private String httpPrefix = "http://localhost:8080";

  // Maps a workspace name to the name of the hosting environment
  private final Map<String, String> workspaceEnvironmentMap;

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

  private HypermediaArtifactRegistry() {
    workspaceEnvironmentMap = new Hashtable<>();
    artifactSemanticTypes = new Hashtable<>();
    artifactTemplateDescriptions = new Hashtable<>();
    artifactActionRouter = new Hashtable<>();
    artifactAPIKeys = new Hashtable<>();
    bodyArtifacts = new Hashtable<>();
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

  public void registerBodyArtifact(HypermediaAgentBodyArtifact bodyArtifact){
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
  }

  public void registerBodyArtifact(WorkspaceId workspaceId, AgentId agentId){
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

  }

  public void addWorkspace(String envName, String wkspName) {
    workspaceEnvironmentMap.put(wkspName, envName);
  }

  public Optional<String> getEnvironmentForWorkspace(String wkspName) {
    String envName = workspaceEnvironmentMap.get(wkspName);
    return envName == null ? Optional.empty() : Optional.of(envName);
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

  public String getHttpEnvironmentsPrefix() {
    return getHttpPrefix() + "/environments/";
  }

  public String getHttpWorkspacesPrefix(String envId) {
    return getHttpEnvironmentsPrefix() + envId + "/workspaces/";
  }

  public String getHttpArtifactsPrefix(String wkspName) {
    Optional<String> envId = this.getEnvironmentForWorkspace(wkspName);

    if (envId.isPresent()) {
      return getHttpWorkspacesPrefix(envId.get()) + wkspName + "/artifacts/";
    }

    throw new IllegalArgumentException("Workspace " + wkspName + " not found in any environment.");
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
}
