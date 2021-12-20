package org.hyperagents.yggdrasil.cartago;

import cartago.AgentId;
import cartago.WorkspaceId;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import javafx.util.Pair;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HypermediaAgentBodyArtifactRegistry {
  private static HypermediaAgentBodyArtifactRegistry registry;

  private String httpPrefix = "http://localhost:8080";

  private final Map<String, String> artifactActionRouter;

  private final Map<String, String> artifactAPIKeys;

  private final Map<String, String> artifactTemplateDescriptions;

  private final Map<String, HypermediaBodyArtifact> bodyArtifacts;

  private final Map<Pair<AgentId, WorkspaceId>, String> agentArtifacts;

  private int n;

  private HypermediaAgentBodyArtifactRegistry(){
    this.artifactActionRouter = new Hashtable<>();
    this.artifactAPIKeys = new Hashtable<>();
    artifactTemplateDescriptions = new Hashtable<>();
    bodyArtifacts = new Hashtable<>();
    agentArtifacts = new Hashtable<>();
    n = 1;
  }

  public static synchronized HypermediaAgentBodyArtifactRegistry getInstance() {
    if (registry == null) {
      registry = new HypermediaAgentBodyArtifactRegistry();
    }

    return registry;
  }

  public void register(HypermediaBodyArtifact artifact){
    String artifactTemplate = artifact.getArtifactId().getName();
    if (!bodyArtifacts.containsKey(artifactTemplate)) {
      bodyArtifacts.put(artifactTemplate, artifact);
    }
    String  description = artifact.getHypermediaDescription();
    artifactTemplateDescriptions.put(artifactTemplate, description);

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

  public String getName(){
    String s ="hypermedia_body_"+n;
    this.n = n +1;
    return s;
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

  /*public String getHttpArtifactsPrefix(String wkspName) {
    //Optional<String> envId = this.getEnvironmentForWorkspace(wkspName);
    Optional<String> envId = HypermediaArtifactRegistry.getInstance().getEnvironmentForWorkspace(wkspName);

    if (envId.isPresent()) {
      return getHttpWorkspacesPrefix(envId.get()) + wkspName + "/bodies/";
    }

    throw new IllegalArgumentException("Workspace " + wkspName + " not found in any environment.");
  }*/

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

  public void setArtifact(AgentId agentId, WorkspaceId workspaceId, String bodyName){
    Pair<AgentId, WorkspaceId> pair = new Pair(agentId, workspaceId);
    this.agentArtifacts.put(pair, bodyName);
  }

  public String getArtifact(AgentId agentId, WorkspaceId workspaceId){
    Pair<AgentId, WorkspaceId> pair = new Pair(agentId, workspaceId);
    return this.agentArtifacts.get(pair);

  }

  public boolean hasArtifact(AgentId agentId, WorkspaceId workspaceId){
    Pair<AgentId, WorkspaceId> pair = new Pair(agentId, workspaceId);
    boolean b = this.agentArtifacts.containsKey(pair);
    return b;

  }
}
