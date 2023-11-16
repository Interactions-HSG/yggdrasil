package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.Artifact;
import cartago.ArtifactId;
import cartago.CartagoException;
import cartago.WorkspaceId;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.security.NoSecurityScheme;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifactRegistry;

public abstract class HypermediaArtifact extends Artifact {
  private final ListMultimap<String, ActionAffordance> actionAffordances =
      Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);
  private final Model metadata = new LinkedHashModel();
  private final Set<String> feedbackActions = new HashSet<>();
  private final Map<String, UnaryOperator<Object>> responseConverterMap = new HashMap<>();
  private SecurityScheme securityScheme = new NoSecurityScheme();

  /**
   * Retrieves a hypermedia description of the artifact's interface. Current implementation is based
   * on the W3C Web of Things <a href="https://www.w3.org/TR/wot-thing-description/">Thing Description</a>.
   *
   * @return An RDF description of the artifact and its interface.
   */
  public String getHypermediaDescription() {
    final var tdBuilder =
        new ThingDescription.Builder(this.getArtifactName())
                            .addSecurityScheme(this.securityScheme)
                            .addSemanticType("https://purl.org/hmas/core/Artifact")
                            .addSemanticType(this.getSemanticType())
                            .addThingURI(this.getArtifactUri())
                            .addGraph(this.metadata);
    this.actionAffordances.values().forEach(tdBuilder::addAction);

    return new TDGraphWriter(tdBuilder.build())
      .setNamespace("td", "https://www.w3.org/2019/wot/td#")
      .setNamespace("htv", "http://www.w3.org/2011/http#")
      .setNamespace("hctl", "https://www.w3.org/2019/wot/hypermedia#")
      .setNamespace("wotsec", "https://www.w3.org/2019/wot/security#")
      .setNamespace("dct", "http://purl.org/dc/terms/")
      .setNamespace("js", "https://www.w3.org/2019/wot/json-schema#")
      .setNamespace("hmas", "https://purl.org/hmas/core/")
      .write();
  }

  /**
   * Retrieves the CArtAgO ArtifactId of this artifact.
   *
   * @return A CArtAgO ArtifactId
   */
  public ArtifactId getArtifactId() {
    return this.getId();
  }

  @Override
  protected void setupOperations() throws CartagoException {
    super.setupOperations();

    this.registerInteractionAffordances();
    HypermediaArtifactRegistry.getInstance().register(this);
  }

  protected abstract void registerInteractionAffordances();

  protected String getArtifactName() {
    return this.getId().getName();
  }

  protected String getArtifactUri() {
    return HypermediaArtifactRegistry.getInstance()
                                     .getHttpArtifactsPrefix(
                                       this.getId().getWorkspaceId().getName()
                                     )
           + this.getArtifactName();
  }

  protected WorkspaceId getWorkspaceId() {
    return this.getArtifactId().getWorkspaceId();
  }

  protected final void registerActionAffordance(
      final String actionClass,
      final String actionName,
      final String relativeUri
  ) {
    this.registerActionAffordance(actionClass, actionName, relativeUri, null);
  }

  protected final void registerActionAffordance(
      final String actionClass,
      final String actionName,
      final String relativeUri,
      final DataSchema inputSchema
  ) {
    this.registerActionAffordance(actionClass, actionName, "POST", relativeUri, inputSchema);
  }

  protected final void registerActionAffordance(
      final String actionClass,
      final String actionName,
      final String methodName,
      final String relativeUri,
      final DataSchema inputSchema
  ) {
    final var actionBuilder =
        new ActionAffordance
          .Builder(
            actionName,
            new Form.Builder(this.getArtifactUri() + relativeUri)
                    .setMethodName(methodName)
                    .build()
          )
          .addSemanticType(actionClass)
          .addTitle(actionName);

    this.registerActionAffordance(
        actionName,
        Optional.ofNullable(inputSchema)
                .map(actionBuilder::addInputSchema)
                .orElse(actionBuilder)
                .build()
    );
  }

  protected final void registerActionAffordance(
      final String actionName,
      final ActionAffordance action
  ) {
    this.actionAffordances.put(actionName, action);
  }

  protected final void registerFeedbackParameter(final String actionName) {
    this.feedbackActions.add(actionName);
  }

  protected final void registerFeedbackParameter(
      final String actionName,
      final UnaryOperator<Object> responseConverter
  ) {
    this.feedbackActions.add(actionName);
    this.responseConverterMap.put(actionName, responseConverter);
  }

  public Map<String, UnaryOperator<Object>> getResponseConverterMap() {
    return new HashMap<>(this.responseConverterMap);
  }

  public Set<String> getFeedbackActions() {
    return new HashSet<>(this.feedbackActions);
  }

  protected final void setSecurityScheme(final SecurityScheme scheme) {
    this.securityScheme = scheme;
  }

  protected final void addMetadata(final Model model) {
    this.metadata.addAll(model);
  }

  public Map<String, List<ActionAffordance>> getActionAffordances() {
    return this.actionAffordances
               .asMap()
               .entrySet()
               .stream()
               .collect(Collectors.toMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue())));
  }

  private String getSemanticType() {
    return HypermediaArtifactRegistry.getInstance()
                                     .getArtifactSemanticType(this.getClass().getCanonicalName())
                                     .orElseThrow(
                                       () -> new RuntimeException("Artifact was not registered!")
                                     );
  }
}
