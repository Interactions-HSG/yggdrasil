package org.hyperagents.yggdrasil.cartago;

import cartago.AgentId;
import cartago.CartagoEnvironment;
import cartago.Workspace;
import cartago.WorkspaceDescriptor;
import cartago.WorkspaceId;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * A singleton used to manage CArtAgO artifacts. An equivalent implementation can be obtained with
 * local maps in Vert.x. Can be refactored using async shared maps to run over a cluster.
 */
@SuppressWarnings("PMD.ReplaceHashtableWithMap")
public final class HypermediaArtifactRegistry {
  private static HypermediaArtifactRegistry REGISTRY;

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
  private final Map<String, String> artifactApiKeys;
  private final SetMultimap<WorkspaceId, AgentId> bodyArtifacts;
  private final Map<String, HypermediaInterface> interfaceMap;
  private final Map<String, HypermediaInterfaceConstructor> interfaceConstructorMap;
  private final Map<String, String> artifactNames;
  private final Map<Pair<AgentId, WorkspaceId>, String> agentArtifacts;
  private final Map<String, String> hypermediaNames;
  private final SetMultimap<String, String> feedbackActions;
  private final Map<String, Map<String, UnaryOperator<Object>>> feedbackResponseConverters;
  private int n = 1;
  private String httpPrefix = "http://localhost:8080";

  private HypermediaArtifactRegistry() {
    this.artifactSemanticTypes = new Hashtable<>();
    this.artifactTemplateDescriptions = Collections.synchronizedMap(new HashMap<>());
    this.artifactActionRouter = Collections.synchronizedMap(new HashMap<>());
    this.artifactApiKeys = Collections.synchronizedMap(new HashMap<>());
    this.bodyArtifacts =
      Multimaps.synchronizedSetMultimap(Multimaps.newSetMultimap(new HashMap<>(), HashSet::new));
    this.interfaceMap = new Hashtable<>();
    this.interfaceConstructorMap = Collections.synchronizedMap(new HashMap<>());
    this.artifactNames = Collections.synchronizedMap(new HashMap<>());
    this.agentArtifacts = Collections.synchronizedMap(new HashMap<>());
    this.hypermediaNames = Collections.synchronizedMap(new HashMap<>());
    this.feedbackActions =
      Multimaps.synchronizedSetMultimap(Multimaps.newSetMultimap(new HashMap<>(), HashSet::new));
    this.feedbackResponseConverters = Collections.synchronizedMap(new HashMap<>());
  }

  @SuppressFBWarnings({"MS_EXPOSE_REP"})
  public static synchronized HypermediaArtifactRegistry getInstance() {
    if (REGISTRY == null) {
      REGISTRY = new HypermediaArtifactRegistry();
    }
    return REGISTRY;
  }

  public void register(final HypermediaArtifact artifact) {
    final var artifactTemplate = artifact.getArtifactId().getName();
    this.artifactTemplateDescriptions.put(artifactTemplate, artifact.getHypermediaDescription());
    artifact.getActionAffordances()
            .entrySet()
            .stream()
            .flatMap(actionEntry -> actionEntry.getValue()
                                               .stream()
                                               .map(action -> Map.entry(
                                                 actionEntry.getKey(),
                                                 action
                                               )))
            .forEach(action -> action.getValue().getFirstForm().ifPresent(value -> {
              if (value.getMethodName().isPresent()) {
                this.artifactActionRouter.put(
                    value.getMethodName().get() + value.getTarget(),
                    action.getKey()
                );
              }
            }));
    this.feedbackActions.putAll(artifactTemplate, artifact.getFeedbackActions());
    this.feedbackResponseConverters.put(artifactTemplate, artifact.getResponseConverterMap());
  }

  public void register(final HypermediaInterface hypermediaInterface) {
    final var artifactTemplate = hypermediaInterface.getHypermediaArtifactName();
    this.artifactTemplateDescriptions.put(
      artifactTemplate,
      hypermediaInterface.getHypermediaDescription()
    );
    hypermediaInterface.getActions()
                       .entrySet()
                       .stream()
                       .flatMap(actionEntry -> actionEntry.getValue()
                                                          .stream()
                                                          .map(action -> Map.entry(
                                                            actionEntry.getKey(),
                                                            action
                                                          )))
                       .forEach(action -> action.getValue().getFirstForm().ifPresent(value -> {
                         if (value.getMethodName().isPresent()) {
                           this.artifactActionRouter.put(
                               value.getMethodName().get() + value.getTarget(),
                               action.getKey()
                           );
                         }
                       }));
    final var artifactName = hypermediaInterface.getActualArtifactName();
    this.interfaceMap.put(artifactName, hypermediaInterface);
    this.artifactNames.put(artifactTemplate, artifactName);
    this.feedbackActions.putAll(artifactTemplate, hypermediaInterface.getFeedbackActions());
    this.feedbackResponseConverters.put(
      artifactTemplate,
      hypermediaInterface.getResponseConverterMap()
    );
  }

  public void registerName(final String bodyName, final String hypermediaName) {
    this.hypermediaNames.put(bodyName, hypermediaName);
  }

  public String getHypermediaName(final String bodyName) {
    return this.hypermediaNames.get(bodyName);
  }

  public void registerInterfaceConstructor(
    final String artifactClass,
    final HypermediaInterfaceConstructor interfaceConstructor
  ) {
    this.interfaceConstructorMap.put(artifactClass, interfaceConstructor);
  }

  public HypermediaInterfaceConstructor getInterfaceConstructor(final String artifactClass) {
    return this.interfaceConstructorMap.get(artifactClass);
  }

  public boolean hasInterfaceConstructor(final String artifactClass) {
    return this.interfaceConstructorMap.containsKey(artifactClass);
  }

  public void addArtifactTemplate(final String key, final String value) {
    this.artifactSemanticTypes.put(key, value);
  }

  public void addArtifactTemplates(final JsonObject artifactTemplates) {
    Optional.ofNullable(artifactTemplates)
            .ifPresent(t -> t.forEach(
              e -> this.artifactSemanticTypes.put(e.getKey(), (String) e.getValue())
            ));
  }

  public Set<String> getArtifactTemplates() {
    return new HashSet<>(this.artifactSemanticTypes.keySet());
  }

  public Optional<String> getArtifactSemanticType(final String artifactTemplate) {
    return this.artifactSemanticTypes
               .values()
               .stream()
               .filter(t -> t.equals(artifactTemplate))
               .findFirst();
  }

  public Optional<String> getArtifactTemplate(final String artifactClass) {
    return Optional.ofNullable(this.artifactSemanticTypes.get(artifactClass));
  }

  public String getArtifactDescription(final String artifactName) {
    return this.artifactTemplateDescriptions.get(artifactName);
  }

  public String getActionName(final String method, final String requestUri) {
    return this.artifactActionRouter.get(method + requestUri);
  }

  public void setApiKeyForArtifact(final String artifactId, final String apiKey) {
    this.artifactApiKeys.put(artifactId, apiKey);
  }

  public String getApiKeyForArtifact(final String artifactId) {
    return this.artifactApiKeys.get(artifactId);
  }

  public void setHttpPrefix(final String prefix) {
    this.httpPrefix = prefix;
  }

  public String getHttpPrefix() {
    return this.httpPrefix;
  }

  public String getHttpWorkspacesPrefix() {
    return this.httpPrefix + "/workspaces/";
  }

  public String getHttpArtifactsPrefix(final String workspaceName) {
    return this.httpPrefix + "/workspaces/" + workspaceName + "/artifacts/";
  }

  public Optional<Workspace> getWorkspaceFromName(final String workspaceName) {
    final var workspaces = new HashSet<Workspace>();
    workspaces.add(CartagoEnvironment.getInstance().getRootWSP().getWorkspace());
    for (final var workspace : workspaces) {
      if (Objects.equals(workspace.getId().getName(), workspaceName)
          || Objects.equals(workspace.getId().getFullName(), workspaceName)) {
        return Optional.of(workspace);
      } else {
        workspaces.remove(workspace);
        workspaces.addAll(
          workspace.getChildWSPs()
                   .stream()
                   .map(WorkspaceDescriptor::getWorkspace)
                   .toList()
        );
      }
    }
    return Optional.empty();
  }

  public boolean hasHypermediaAgentBody(final AgentId agentId, final WorkspaceId workspaceId) {
    return this.bodyArtifacts.containsKey(workspaceId)
           && this.bodyArtifacts.get(workspaceId).contains(agentId);
  }

  public boolean hasHypermediaInterface(final String artifactName) {
    return this.interfaceMap.containsKey(artifactName);
  }

  public HypermediaInterface getHypermediaInterface(final String artifactName) {
    return this.interfaceMap.get(artifactName);
  }

  public boolean hasOtherName(final String hypermediaArtifactName) {
    return this.artifactNames.containsKey(hypermediaArtifactName);
  }

  public String getArtifactWithHypermediaInterfaces() {
    return this.interfaceMap.keySet().toString();
  }

  public String getActualName(final String hypermediaArtifactName) {
    return this.artifactNames.get(hypermediaArtifactName);
  }

  public void setArtifact(
    final AgentId agentId,
    final WorkspaceId workspaceId,
    final String bodyName
  ) {
    this.agentArtifacts.put(new ImmutablePair<>(agentId, workspaceId), bodyName);
  }

  public String getArtifact(final AgentId agentId, final WorkspaceId workspaceId) {
    return this.agentArtifacts.get(new ImmutablePair<>(agentId, workspaceId));
  }

  public boolean hasArtifact(final AgentId agentId, final WorkspaceId workspaceId) {
    return this.agentArtifacts.containsKey(new ImmutablePair<>(agentId, workspaceId));
  }

  public String getName() {
    this.n++;
    return "hypermedia_body_" + this.n;
  }

  public boolean hasFeedbackParam(final String artifactName, final String action) {
    final var registry = HypermediaAgentBodyArtifactRegistry.getInstance();
    return this.feedbackActions
               .get(
                 registry.isBodyArtifact(artifactName)
                 ? registry.getHypermediaName(artifactName)
                 : artifactName
               )
               .contains(action);
  }

  public boolean hasFeedbackResponseConverter(final String artifactName, final String action) {
    final var registry = HypermediaAgentBodyArtifactRegistry.getInstance();
    return this.feedbackResponseConverters
               .get(
                 registry.isBodyArtifact(artifactName)
                 ? registry.getHypermediaName(artifactName)
                 : artifactName
               )
               .containsKey(action);
  }

  public UnaryOperator<Object> getFeedbackResponseConverter(
    final String artifactName,
    final String action
  ) {
    final var registry = HypermediaAgentBodyArtifactRegistry.getInstance();
    return this.feedbackResponseConverters
               .get(
                 registry.isBodyArtifact(artifactName)
                 ? registry.getHypermediaName(artifactName)
                 : artifactName
               )
               .get(action);
  }

  public String getArtifactUri(final String workspaceName, final String artifactName) {
    return this.httpPrefix + "/workspaces/" + workspaceName + "/artifacts/" + artifactName;
  }
}
