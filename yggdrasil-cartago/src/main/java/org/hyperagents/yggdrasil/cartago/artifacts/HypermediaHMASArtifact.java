package org.hyperagents.yggdrasil.cartago.artifacts;

import static org.hyperagents.yggdrasil.utils.JsonObjectUtils.parseInput;

import cartago.Artifact;
import cartago.ArtifactId;
import cartago.CartagoException;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphReader;
import ch.unisg.ics.interactions.hmas.interaction.shapes.IOSpecification;
import ch.unisg.ics.interactions.hmas.interaction.shapes.ListSpecification;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.ActionSpecification;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.Form;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.Signifier;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
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
import org.hyperagents.yggdrasil.utils.impl.RepresentationFactoryHMASImpl;

/**
 * Abstract Class that implements common functionality of all HypermediaHMASArtifacts.
 */
public abstract class HypermediaHMASArtifact extends Artifact implements HypermediaArtifact {
  private static final String DEFAULT_CONFIG_VALUE = "default";

  private final ListMultimap<String, Object> signifiers =
      Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);
  private final Model metadata = new LinkedHashModel();
  private final Map<String, Integer> feedbackActions = new HashMap<>();
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
      new RepresentationFactoryHMASImpl(this.httpConfig, this.notificationConfig);

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
        semanticType,
        this.metadata,
        this.signifiers,
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

  /**
   * Retrieves the methodname and target of an action.
   *
   * @param action Object that is of the correct signifier type.
   * @return an Optional of the Methodname and target if it is present.
   */
  public final Optional<String> getMethodNameAndTarget(final Object action) {
    final var signifier = (Signifier) action;
    final var form = signifier.getActionSpecification().getForms().stream().findFirst();

    if (form.isPresent()) {
      final var formValue = form.get();
      if (formValue.getMethodName().isPresent()) {
        return Optional.of(formValue.getMethodName().get() + formValue.getTarget());
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
    return this.signifiers
        .asMap()
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue())));
  }

  protected abstract void registerInteractionAffordances();

  protected URI getBaseUri() {
    return URI.create(this.httpConfig.getBaseUriTrailingSlash());
  }

  private String getWorkspaceName() {
    return this.getId().getWorkspaceId().getName();
  }

  private String getArtifactName() {
    return this.getId().getName();
  }

  @Override
  protected void setupOperations() throws CartagoException {
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
          new RepresentationFactoryHMASImpl(this.httpConfig, this.notificationConfig);
    }
    this.registerInteractionAffordances();
  }

  protected final String getArtifactUri() {
    return this.httpConfig.getArtifactUriTrailingSlash(
        this.getId().getWorkspaceId().getName(),
        this.getId().getName()
    );
  }

  protected final void registerSignifier(
      final String actionClass,
      final String actionName,
      final String relativeUri
  ) {
    this.registerSignifier(actionClass, actionName, relativeUri, null);
  }

  protected final void registerSignifier(
      final String actionClass,
      final String actionName,
      final String relativeUri,
      final IOSpecification inputSchema
  ) {
    this.registerSignifier(actionClass, actionName, "POST", relativeUri, inputSchema, null);
  }

  protected final void registerSignifier(
      final String actionClass,
      final String actionName,
      final String relativeUri,
      final IOSpecification inputSchema,
      final IOSpecification outputSchema
  ) {
    this.registerSignifier(actionClass, actionName, "POST", relativeUri, inputSchema, outputSchema);
  }

  protected final void registerSignifier(
      final String actionClass,
      final String actionName,
      final String methodName,
      final String relativeUri,
      final IOSpecification inputSchema,
      final IOSpecification outputSchema
  ) {


    // TODO: set content type dynamically
    // TODO: set input correctly
    final var form = new Form.Builder(this.getArtifactUri() + relativeUri)
        .setIRIAsString(this.getArtifactUri() + "#" + actionName)
        .setMethodName(methodName)
        .setContentType("application/json")
        .build();

    final var actionSpecification = new ActionSpecification.Builder(form).addRequiredSemanticTypes(
        Collections.singleton(actionClass)).addSemanticType(actionClass);

    if (inputSchema != null) {
      actionSpecification.setInputSpecification(inputSchema);
    }

    if (outputSchema != null) {
      actionSpecification.setOutputSpecification(outputSchema);
    }

    final var signifier = new Signifier.Builder(actionSpecification.build())
        .setIRIAsString(this.getArtifactUri() + "#" + actionName + "-Signifier")
        .build();

    this.registerSignifier(
        actionName,
        signifier
    );
  }

  protected final void registerSignifier(
      final String actionName,
      final Signifier signifier
  ) {
    this.signifiers.put(actionName, signifier);
  }

  protected final void registerFeedbackParameter(final String actionName) {
    final int params = this.feedbackActions.getOrDefault(actionName, 0);
    this.feedbackActions.put(actionName, params + 1);
  }

  protected final void addMetadata(final Model model) {
    this.metadata.addAll(model);
  }

  /**
   * Fuction that given some raw input processes it so that it is usable by operations.
   *
   * @param storeResponse Representation of the Artifact in HMAS Ontology.
   * @param actionName    Name of the action to be executed.
   * @param context       Context of the http request, represents the wanted input to the action.
   * @return String that represents correct input for the given action.
   */
  public Optional<String> handleInput(final String storeResponse, final String actionName,
                                      final String context) {
    final var signifier = getSignifier(storeResponse, actionName);
    Optional<String> description = Optional.empty();
    if (signifier.isPresent()
        &&
        signifier.get().getActionSpecification().getInputSpecification().isPresent()) {
      final JsonElement jsonElement = JsonParser.parseString(context);
      final var input = signifier.get().getActionSpecification().getInputSpecification().get();
      final ListSpecification listSpecification = (ListSpecification) input;
      description = CartagoDataBundle.toJson(
          parseInput(jsonElement, listSpecification, new ArrayList<>())
      ).describeConstable();
    }
    return description;
  }

  /**
   * Returns the number of parameters that are expected for the given action.
   *
   * @param storeResponse Representation of the artifact in HMAS ontology.
   * @param actionName    Name of the action that we want to execute.
   * @return An integer.
   */
  public Integer handleOutputParams(final String storeResponse, final String actionName) {
    final var signifier = getSignifier(storeResponse, actionName);
    if (signifier.isPresent()
        &&
        signifier.get().getActionSpecification().getOutputSpecification().isPresent()) {
      final var output = signifier.get().getActionSpecification().getOutputSpecification().get();
      final ListSpecification listSpecification = (ListSpecification) output;
      return getLengthOfQualifiedValueSpecificationList(listSpecification);
    }
    return 0;
  }

  private Optional<Signifier> getSignifier(final String storeResponse, final String actionName) {
    final var workspaceName = getWorkspaceName();
    final var artifactName = getArtifactName();

    final var artifactIri = this.httpConfig
        .getArtifactUriTrailingSlash(workspaceName, artifactName);

    final var signifierIri = artifactIri + "#" + actionName + "-Signifier";

    return ResourceProfileGraphReader.readFromString(storeResponse).getExposedSignifiers().stream()
        .filter(sig -> sig.getIRIAsString().isPresent())
        .filter(sig -> sig.getIRIAsString().get().equals(signifierIri))
        .findFirst();
  }

  private int getLengthOfQualifiedValueSpecificationList(
      final ListSpecification listSpecification) {
    final var members = listSpecification.getMemberSpecifications();
    int lengthOfList = 0;

    for (final var member : members) {
      if (member instanceof ListSpecification) {
        lengthOfList += getLengthOfQualifiedValueSpecificationList((ListSpecification) member);
      } else {
        lengthOfList++;
      }
    }

    return lengthOfList;
  }

}
