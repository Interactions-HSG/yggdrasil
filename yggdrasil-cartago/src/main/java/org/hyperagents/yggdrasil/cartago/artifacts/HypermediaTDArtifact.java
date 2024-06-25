package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.Artifact;
import cartago.ArtifactId;
import cartago.CartagoException;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.security.NoSecurityScheme;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.gson.JsonParser;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.hyperagents.yggdrasil.cartago.CartagoDataBundle;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifactRegistry;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.RepresentationFactory;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.RepresentationFactoryTDImplt;

import java.net.URI;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public abstract class HypermediaTDArtifact extends Artifact implements HypermediaArtifact {
  private static final String DEFAULT_CONFIG_VALUE = "default";

  private final ListMultimap<String, ActionAffordance> actionAffordances =
    Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);
  private final Model metadata = new LinkedHashModel();
  private final Map<String, Integer> feedbackActions = new HashMap<>();
  private final Map<String, UnaryOperator<Object>> responseConverterMap = new HashMap<>();
  private HttpInterfaceConfig httpConfig = Vertx.currentContext()
    .owner()
    .sharedData()
    .<String, HttpInterfaceConfig>getLocalMap("http-config")
    .get(DEFAULT_CONFIG_VALUE);
  private RepresentationFactory representationFactory =
    new RepresentationFactoryTDImplt(this.httpConfig);
  private SecurityScheme securityScheme = new NoSecurityScheme();

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
      this.securityScheme,
      HypermediaArtifactRegistry.getInstance()
        .getArtifactSemanticType(this.getClass().getCanonicalName())
        .orElseThrow(
          () -> new RuntimeException("Artifact was not registered!")
        ),
      this.metadata,
      this.actionAffordances
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

  public final Map<String, Integer> getFeedbackActions() {
    return feedbackActions;
  }

  public final Map<String, List<ActionAffordance>> getActionAffordances() {
    return this.actionAffordances
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
      this.representationFactory = new RepresentationFactoryTDImplt(this.httpConfig);
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
    final int params = this.feedbackActions.getOrDefault(actionName, 0);
    this.feedbackActions.put(actionName, params + 1);
  }

  protected final void registerFeedbackParameters(final String actionName, int numberOfParameters) {
    final int params = this.feedbackActions.getOrDefault(actionName, 0);
    this.feedbackActions.put(actionName,params + numberOfParameters);
  }

  protected final void registerFeedbackParameter(
    final String actionName,
    final UnaryOperator<Object> responseConverter
  ) {
    registerFeedbackParameter(actionName);
    this.responseConverterMap.put(actionName, responseConverter);
  }

  protected final void setSecurityScheme(final SecurityScheme scheme) {
    this.securityScheme = scheme;
  }

  protected final void addMetadata(final Model model) {
    this.metadata.addAll(model);
  }

  public Optional<String> handleAction(String storeResponse, String actionName, String context)
  {
    return TDGraphReader
      .readFromString(ThingDescription.TDFormat.RDF_TURTLE, storeResponse)
      .getActions()
      .stream()
      .filter(
        action -> action.getTitle().isPresent()
          && action.getTitle().get().equals(actionName)
      )
      .findFirst()
      .flatMap(ActionAffordance::getInputSchema)
      .filter(inputSchema -> inputSchema.getDatatype().equals(DataSchema.ARRAY))
      .map(inputSchema -> CartagoDataBundle.toJson(
        ((ArraySchema) inputSchema)
          .parseJson(JsonParser.parseString(context))
      ));
  }

}

