package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.Artifact;
import cartago.ArtifactId;
import cartago.CartagoException;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.ActionSpecification;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.Form;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.InputSpecification;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.Signifier;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import io.vertx.core.json.JsonObject;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifactRegistry;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.RepresentationFactory;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.RepresentationFactoryImpl;

public abstract class HypermediaArtifact extends Artifact {
  private final ListMultimap<String, Signifier> signifiers =
      Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);
  private final Model metadata = new LinkedHashModel();
  private final Set<String> feedbackActions = new HashSet<>();
  private final Map<String, UnaryOperator<Object>> responseConverterMap = new HashMap<>();
  private HttpInterfaceConfig httpConfig = new HttpInterfaceConfigImpl(JsonObject.of());
  private RepresentationFactory representationFactory =
      new RepresentationFactoryImpl(this.httpConfig);


  /**
   * Retrieves a hypermedia description of the artifact's interface. Current implementation is based
   * on the W3C Web of Things <a href="https://www.w3.org/TR/wot-thing-description/">Thing Description</a>.
   *
   * @return An RDF description of the artifact and its interface.
   */
  public final String getHypermediaDescription() {
    return this.representationFactory.createArtifactRepresentation(
      this.getId().getWorkspaceId().getName(),
      this.getId().getName(),
      HypermediaArtifactRegistry.getInstance()
                                .getArtifactSemanticType(this.getClass().getCanonicalName())
                                .orElseThrow(
                                  () -> new RuntimeException("Artifact was not registered!")
                                ),
      this.metadata,
      this.signifiers
    );
  }

  /**
   * Retrieves the CArtAgO ArtifactId of this artifact.
   *
   * @return A CArtAgO ArtifactId
   */
  public final ArtifactId getArtifactId() {
    return this.getId();
  }

  public final Map<String, UnaryOperator<Object>> getResponseConverterMap() {
    return new HashMap<>(this.responseConverterMap);
  }

  public final Set<String> getFeedbackActions() {
    return new HashSet<>(this.feedbackActions);
  }

  public final Map<String, List<Signifier>> getSignifiers() {
    return this.signifiers
               .asMap()
               .entrySet()
               .stream()
               .collect(Collectors.toMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue())));
  }

  protected abstract void registerInteractionAffordances();

  protected URI getBaseUri() {
    return URI.create(this.httpConfig.getBaseUri());
  }

  @Override
  protected void setupOperations() throws CartagoException {
    super.setupOperations();
    final var baseUri = this.getBaseUri();
    if (!baseUri.toString().equals(this.httpConfig.getBaseUri())) {
      this.httpConfig = new HttpInterfaceConfigImpl(JsonObject.of(
        "http-config",
        JsonObject.of(
          "host",
          baseUri.getHost(),
          "port",
          baseUri.getPort(),
          "base-uri",
          baseUri.toString()
        )
      ));
      this.representationFactory = new RepresentationFactoryImpl(this.httpConfig);
    }
    this.registerInteractionAffordances();
    HypermediaArtifactRegistry.getInstance().register(this);
  }

  protected final String getArtifactUri() {
    return this.httpConfig.getArtifactUri(
      this.getId().getWorkspaceId().getName(),
      this.getId().getName()
    );
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


    // TODO: set content type dynamically
    // TODO: set input correctly
    final var form = new Form.Builder(this.getArtifactUri() + relativeUri)
      .setIRIAsString(this.getArtifactUri() + "#" + actionName)
      .setMethodName(methodName)
      .setContentType("application/json")
      .build();

    final var actionSpecification = new ActionSpecification.Builder(form);
    if (inputSchema != null) {
      var inputSpecification = new InputSpecification.Builder()
        .build();
      actionSpecification.setRequiredInput(inputSpecification);
    }

    final var signifier = new Signifier.Builder(actionSpecification.build())
      .setIRIAsString(this.getArtifactUri() + "#" + actionClass)
      .build();

    this.registerActionAffordance(
        actionName,
        signifier
    );
  }

  protected final void registerActionAffordance(
      final String actionName,
      final Signifier signifier
  ) {
    this.signifiers.put(actionName, signifier);
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

  protected final void addMetadata(final Model model) {
    this.metadata.addAll(model);
  }
}
