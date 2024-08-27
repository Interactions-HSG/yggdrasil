package org.hyperagents.yggdrasil.cartago;

import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.hyperagents.yggdrasil.cartago.artifacts.HypermediaArtifact;

/**
 * A singleton used to manage CArtAgO artifacts. An equivalent implementation can be obtained with
 * local maps in Vert.x. Can be refactored using async shared maps to run over a cluster.
 */
@SuppressWarnings("PMD.ReplaceHashtableWithMap")
public final class HypermediaArtifactRegistry {
  // Maps the canonical name of a CArtAgO artifact class to the corresponding HypermediaArtifact
  private final Map<String, HypermediaArtifact> artifacts;

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
  private final Map<String, String> artifactNames;
  private int counter;

  public HypermediaArtifactRegistry() {
    this.artifactSemanticTypes = new ConcurrentHashMap<>();
    this.artifacts = Collections.synchronizedMap(new HashMap<>());
    this.artifactTemplateDescriptions = Collections.synchronizedMap(new HashMap<>());
    this.artifactActionRouter = Collections.synchronizedMap(new HashMap<>());
    this.artifactApiKeys = Collections.synchronizedMap(new HashMap<>());
    this.artifactNames = Collections.synchronizedMap(new HashMap<>());
    this.counter = 0;
  }

  public void register(final HypermediaArtifact artifact) {
    final var artifactTemplate = artifact.getArtifactId().getName();
    this.artifacts.put(artifactTemplate, artifact);
    this.artifactTemplateDescriptions.put(artifactTemplate, artifact.getHypermediaDescription(
        this
            .getArtifactSemanticType(artifact.getClass().getCanonicalName())
            .orElseThrow(
                () -> new RuntimeException("Artifact was not registered!")
            )));
    artifact.getArtifactActions()
        .entrySet()
        .stream()
        .flatMap(signifierEntry -> signifierEntry.getValue()
            .stream()
            .map(signifier -> Map.entry(
                signifierEntry.getKey(),
                signifier
            )))
        .forEach(signifier -> artifact.getMethodNameAndTarget(signifier.getValue())
            .ifPresent(s -> this.artifactActionRouter.put(
                s,
                signifier.getKey()
            )));
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
        .entrySet()
        .stream()
        .filter(e -> e.getValue().equals(artifactTemplate))
        .map(Map.Entry::getKey)
        .findFirst();
  }

  public Optional<String> getArtifactTemplate(final String artifactSemanticType) {
    return Optional.ofNullable(this.artifactSemanticTypes.get(artifactSemanticType));
  }

  public String getArtifactDescription(final String artifactName) {
    return this.artifactTemplateDescriptions.get(artifactName);
  }

  public String getActionName(final String method) {
    return this.artifactActionRouter.get(method);
  }

  public void setApiKeyForArtifact(final String artifactId, final String apiKey) {
    this.artifactApiKeys.put(artifactId, apiKey);
  }

  public String getApiKeyForArtifact(final String artifactId) {
    return this.artifactApiKeys.get(artifactId);
  }

  public boolean hasOtherName(final String hypermediaArtifactName) {
    return this.artifactNames.containsKey(hypermediaArtifactName);
  }

  public String getActualName(final String hypermediaArtifactName) {
    return this.artifactNames.get(hypermediaArtifactName);
  }

  public String getName() {
    this.counter++;
    return "hypermedia_body_" + this.counter;
  }

  public HypermediaArtifact getArtifact(final String artifactName) {
    return this.artifacts.get(artifactName);
  }
}
