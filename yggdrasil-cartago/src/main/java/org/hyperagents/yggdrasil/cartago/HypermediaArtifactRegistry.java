package org.hyperagents.yggdrasil.cartago;

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
  private int counter;

  /**
   * Constructor.
   */
  public HypermediaArtifactRegistry() {
    this.artifactSemanticTypes = new ConcurrentHashMap<>();
    this.artifacts = Collections.synchronizedMap(new HashMap<>());
    this.artifactTemplateDescriptions = Collections.synchronizedMap(new HashMap<>());
    this.artifactActionRouter = Collections.synchronizedMap(new HashMap<>());
    this.counter = 0;
  }

  /**
   * When a new artifact is created it is then registered in the Registry using this method.
   *
   * @param artifact a HypermediaArtifact.
   */
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

  public Set<String> getArtifactTemplates() {
    return new HashSet<>(this.artifactSemanticTypes.keySet());
  }

  /**
   * Given an artifactTemplate returns its semantic type.
   *
   * @param artifactTemplate String.
   * @return Optional of the semantic type.
   */
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

  public Optional<String> getActionName(final String method) {
    return Optional.ofNullable(this.artifactActionRouter.get(method));
  }

  public String getName() {
    this.counter++;
    return "hypermedia_body_" + this.counter;
  }

  public Optional<HypermediaArtifact> getArtifact(final String artifactName) {
    return Optional.ofNullable(this.artifacts.get(artifactName));
  }
}
