package org.hyperagents.yggdrasil.cartago;

import cartago.AgentId;
import cartago.WorkspaceId;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HypermediaAgentBodyArtifactRegistry {
  private static HypermediaAgentBodyArtifactRegistry REGISTRY;

  private final Map<String, String> artifactActionRouter;
  private final Map<String, String> artifactAPIKeys;
  private final Map<String, String> artifactTemplateDescriptions;
  private final Map<Pair<AgentId, WorkspaceId>, String> agentBodyNames;
  private final Map<String, String> hypermediaNames;
  private String httpPrefix = "http://localhost:8080";
  private int n;

  private HypermediaAgentBodyArtifactRegistry() {
    this.artifactActionRouter = Collections.synchronizedMap(new HashMap<>());
    this.artifactAPIKeys = Collections.synchronizedMap(new HashMap<>());
    this.artifactTemplateDescriptions = Collections.synchronizedMap(new HashMap<>());
    this.agentBodyNames = Collections.synchronizedMap(new HashMap<>());
    this.hypermediaNames = Collections.synchronizedMap(new HashMap<>());
    this.n = 1;
  }

  public static synchronized HypermediaAgentBodyArtifactRegistry getInstance() {
    if (REGISTRY == null) {
      REGISTRY = new HypermediaAgentBodyArtifactRegistry();
    }
    return REGISTRY;
  }


  public void registerName(final String bodyName, final String hypermediaName) {
    this.hypermediaNames.put(bodyName, hypermediaName);
  }

  public String getHypermediaName(final String bodyName) {
    return this.hypermediaNames.get(bodyName);
  }

  public String getName() {
    this.n++;
    return "hypermedia_body_" + this.n;
  }

  public void setHttpPrefix(final String prefix) {
    this.httpPrefix = prefix;
  }

  public String getHttpPrefix() {
    return this.httpPrefix;
  }

  public String getHttpEnvironmentsPrefix() {
    return this.httpPrefix + "/environments/";
  }

  public String getArtifactDescription(final String artifactName) {
    return this.artifactTemplateDescriptions.get(artifactName);
  }

  public String getActionName(final String method, final String requestURI) {
    return this.artifactActionRouter.get(method + requestURI);
  }

  public void setAPIKeyForArtifact(final String artifactId, final String apiKey) {
    this.artifactAPIKeys.put(artifactId, apiKey);
  }

  public String getAPIKeyForArtifact(final String artifactId) {
    return this.artifactAPIKeys.get(artifactId);
  }

  public void setArtifact(
    final AgentId agentId,
    final WorkspaceId workspaceId,
    final String bodyName
  ) {
    this.agentBodyNames.put(new ImmutablePair<>(agentId, workspaceId), bodyName);
  }

  public String getArtifact(final AgentId agentId, final WorkspaceId workspaceId) {
    return this.agentBodyNames.get(new ImmutablePair<>(agentId, workspaceId));
  }

  public boolean hasArtifact(final AgentId agentId, final WorkspaceId workspaceId) {
    return this.agentBodyNames.containsKey(new ImmutablePair<>(agentId, workspaceId));
  }

  public boolean isBodyArtifact(final String artifactName) {
    return this.hypermediaNames.containsKey(artifactName);
  }
}
