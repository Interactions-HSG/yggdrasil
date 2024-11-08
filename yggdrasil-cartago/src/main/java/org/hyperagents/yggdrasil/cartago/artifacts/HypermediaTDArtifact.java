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
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.gson.JsonParser;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.hyperagents.yggdrasil.cartago.CartagoDataBundle;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.RepresentationFactory;
import org.hyperagents.yggdrasil.utils.WebSubConfig;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.RepresentationFactoryTDImplt;

/**
 * Abstract Class that implements common functionality of all HypermediaTDArtifacts.
 */
public abstract class HypermediaTDArtifact extends Artifact implements HypermediaArtifact {
  private static final String DEFAULT_CONFIG_VALUE = "default";

  private final ListMultimap<String, Object> actionAffordances =
      Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);
  private final Model metadata = new LinkedHashModel();
  private final WebSubConfig notificationConfig = Vertx.currentContext()
      .owner()
      .sharedData()
      .<String, WebSubConfig>getLocalMap("notification-config")
      .get(DEFAULT_CONFIG_VALUE);
  private HttpInterfaceConfig httpConfig = Vertx.currentContext()
      .owner()
      .sharedData()
      .<String, HttpInterfaceConfig>getLocalMap("http-config")
      .get(DEFAULT_CONFIG_VALUE);
  private RepresentationFactory representationFactory =
      new RepresentationFactoryTDImplt(this.httpConfig, this.notificationConfig);
  private SecurityScheme securityScheme = SecurityScheme.getNoSecurityScheme();

  private String apiKey;

  /**
   * Retrieves a hypermedia description of the artifact's interface. Current implementation is based
   * on the W3C Web of Things <a href="https://www.w3.org/TR/wot-thing-description/">Thing
   * Description</a>.
   *
   * @return An RDF description of the artifact and its interface.
   */
  public final String getHypermediaDescription(final String semanticType) {
    return this.representationFactory.createArtifactRepresentation(
        this.getId().getWorkspaceId().getName(),
        this.getId().getName(),
        this.securityScheme,
        semanticType,
        this.metadata,
        this.actionAffordances,
        true
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

  public String getApiKey() {
    return this.apiKey;
  }

  public void setApiKey(final String key) {
    this.apiKey = key;
  }

  @Override
  public Optional<String> getMethodNameAndTarget(final Object action) {
    final var ActionAffordance = (ActionAffordance) action;
    if (ActionAffordance.getFirstForm().isPresent()) {
      final var form = ActionAffordance.getFirstForm().get();
      if (form.getMethodName().isPresent()) {
        return Optional.of(form.getMethodName().get() + form.getTarget());
      }
    }
    return Optional.empty();
  }

  /**
   * Returns a map of all actions of the artifact.
   *
   * @return a map of actions.
   */
  public final Map<String, List<Object>> getArtifactActions() {
    return this.actionAffordances
        .asMap()
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue())));
  }

  protected abstract void registerInteractionAffordances();

  protected URI getBaseUri() {
    return URI.create(this.httpConfig.getBaseUriTrailingSlash());
  }

  @Override
  public void setupOperations() throws CartagoException {
    super.setupOperations();
    final var baseUri = this.getBaseUri();
    if (!baseUri.toString().equals(this.httpConfig.getBaseUriTrailingSlash())) {
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
      this.representationFactory =
          new RepresentationFactoryTDImplt(this.httpConfig, this.notificationConfig);
    }
    this.registerInteractionAffordances();
  }

  protected final String getArtifactUri() {
    return this.httpConfig.getArtifactUriTrailingSlash(
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
    this.registerActionAffordance(actionClass, actionName, "POST", relativeUri, inputSchema, null);
  }

  protected final void registerActionAffordance(
      final String actionClass,
      final String actionName,
      final String relativeUri,
      final DataSchema inputSchema,
      final DataSchema outputSchema
  ) {
    this.registerActionAffordance(actionClass, actionName, "POST", relativeUri, inputSchema,
        outputSchema);
  }

  protected final void registerActionAffordance(
      final String actionClass,
      final String actionName,
      final String methodName,
      final String relativeUri,
      final DataSchema inputSchema,
      final DataSchema outputSchema
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

    if (inputSchema != null) {
      actionBuilder.addInputSchema(inputSchema);
    }
    if (outputSchema != null) {
      actionBuilder.addOutputSchema(outputSchema);
    }

    this.registerActionAffordance(
        actionName,
        actionBuilder.build()
    );
  }

  protected final void registerActionAffordance(
      final String actionName,
      final ActionAffordance action
  ) {
    this.actionAffordances.put(actionName, action);
  }

  protected final void setSecurityScheme(final SecurityScheme scheme) {
    this.securityScheme = scheme;
  }

  protected final void addMetadata(final Model model) {
    this.metadata.addAll(model);
  }

  /**
   * Fuction that given some raw input processes it so that it is usable by operations.
   *
   * @param storeResponse Representation of the Artifact in TD Ontology.
   * @param actionName    Name of the action to be executed.
   * @param context       Context of the http request, represents the wanted input to the action.
   * @return String that represents correct input for the given action.
   */
  public Optional<String> handleInput(final String storeResponse, final String actionName,
                                      final String context) {
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

  /**
   * Returns the number of parameters that are expected for the given action.
   *
   * @param storeResponse Representation of the artifact in TD ontology.
   * @param actionName    Name of the action that we want to execute.
   * @return An integer.
   */
  public Integer handleOutputParams(final String storeResponse, final String actionName) {
    final var action =
        TDGraphReader.readFromString(ThingDescription.TDFormat.RDF_TURTLE, storeResponse)
            .getActions()
            .stream()
            .filter(
                a -> a.getTitle().isPresent()
                    && a.getTitle().get().equals(actionName)
            ).findFirst();


    if (action.isPresent() && action.get().getOutputSchema().isPresent()) {
      final var outputSchema = action.get().getOutputSchema().get();
      if (outputSchema.getDatatype().equals(DataSchema.ARRAY)) {
        final var arraySchema = (ArraySchema) outputSchema;
        return arraySchema.getItems().size();
      }
    }
    return 0;
  }

}

