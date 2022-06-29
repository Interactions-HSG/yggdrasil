package org.hyperagents.yggdrasil.cartago;

import cartago.AgentId;
import cartago.WorkspaceId;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import org.apache.commons.lang3.tuple.ImmutablePair;

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

  private final Map<ImmutablePair<AgentId, WorkspaceId>, String> agentArtifacts;

  private final Map<String, String> hypermediaNames;

  private int n;

  private HypermediaAgentBodyArtifactRegistry(){
    this.artifactActionRouter = new Hashtable<>();
    this.artifactAPIKeys = new Hashtable<>();
    artifactTemplateDescriptions = new Hashtable<>();
    agentArtifacts = new Hashtable<>();
    hypermediaNames = new Hashtable<>();
    n = 1;
  }

  public static synchronized HypermediaAgentBodyArtifactRegistry getInstance() {
    if (registry == null) {
      registry = new HypermediaAgentBodyArtifactRegistry();
    }

    return registry;
  }


  public void registerName(String bodyName, String hypermediaName){
    hypermediaNames.put(bodyName, hypermediaName);
  }

  public String getHypermediaName(String bodyName){
    return hypermediaNames.get(bodyName);
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
}
