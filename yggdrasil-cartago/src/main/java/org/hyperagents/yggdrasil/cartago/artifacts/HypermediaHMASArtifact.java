package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.Artifact;
import cartago.ArtifactId;
import cartago.CartagoException;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphReader;
import ch.unisg.ics.interactions.hmas.interaction.shapes.IOSpecification;
import ch.unisg.ics.interactions.hmas.interaction.shapes.QualifiedValueSpecification;
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
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.hyperagents.yggdrasil.cartago.CartagoDataBundle;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.RepresentationFactory;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.RepresentationFactoryHMASImpl;

import static org.hyperagents.yggdrasil.utils.JsonObjectUtils.parseInput;

public abstract class HypermediaHMASArtifact extends Artifact implements HypermediaArtifact {
  private static final String DEFAULT_CONFIG_VALUE = "default";

  private final ListMultimap<String, Object> signifiers =
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
    new RepresentationFactoryHMASImpl(this.httpConfig);

  /**
   * Retrieves a hypermedia description of the artifact's interface. Current implementation is based
   * on the W3C Web of Things <a href="https://www.w3.org/TR/wot-thing-description/">Thing Description</a>.
   *
   * @return An RDF description of the artifact and its interface.
   */
  public final String getHypermediaDescription(final String semanticType) {
    return this.representationFactory.createArtifactRepresentation(
      this.getId().getWorkspaceId().getName(),
      this.getId().getName(),
      semanticType,
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

  public final Map<String, List<Object>> getArtifactActions() {
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
      this.representationFactory = new RepresentationFactoryHMASImpl(this.httpConfig);
    }
    this.registerInteractionAffordances();
  }

  protected final String getArtifactUri() {
    return this.httpConfig.getArtifactUri(
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
    // IO Output
  ) {


    // TODO: set content type dynamically
    // TODO: set input correctly
    final var form = new Form.Builder(this.getArtifactUri() + relativeUri)
      .setIRIAsString(this.getArtifactUri() + "#" + actionName)
      .setMethodName(methodName)
      .setContentType("application/json")
      .build();

    final var actionSpecification = new ActionSpecification.Builder(form).addRequiredSemanticTypes(Collections.singleton(actionClass)).addSemanticType(actionClass);

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

  protected final void registerFeedbackParameters(final String actionName, final int numberOfParameters) {
    final int params = this.feedbackActions.getOrDefault(actionName, 0);
    this.feedbackActions.put(actionName, params + numberOfParameters);
  }

  protected final void registerFeedbackParameter(
    final String actionName,
    final UnaryOperator<Object> responseConverter
  ) {
    registerFeedbackParameter(actionName);
    this.responseConverterMap.put(actionName, responseConverter);
  }

  protected final void addMetadata(final Model model) {
    this.metadata.addAll(model);
  }



  public Optional<String> handleInput(final String storeResponse, final String actionName, final String context) {

    final var workspaceName = this.getId().getWorkspaceId().getName();
    final var artifactName = this.getId().getName();

    final var artifactIri = this.httpConfig.getArtifactUri(workspaceName, artifactName);


    final var signifierIRI = artifactIri + "#" + actionName + "-Signifier";


    final var signifier = ResourceProfileGraphReader.readFromString(storeResponse).getExposedSignifiers().stream()
      .filter(sig -> sig.getIRIAsString().isPresent())
      .filter(sig -> sig.getIRIAsString().get().equals(signifierIRI))
      .findFirst();
    Optional<String> description = Optional.empty();
    if (signifier.isPresent() && signifier.get().getActionSpecification().getInputSpecification().isPresent()) {
      final JsonElement jsonElement = JsonParser.parseString(context);
      final var input = signifier.get().getActionSpecification().getInputSpecification().get();
      final QualifiedValueSpecification qualifiedValueSpecification = (QualifiedValueSpecification) input;
      description = CartagoDataBundle.toJson(
        parseInput(jsonElement, qualifiedValueSpecification, new ArrayList<>())
      ).describeConstable();
    }
    return description;
  }

  public Integer handleOutputParams(final String storeResponse, final String actionName, final String context) {
    final var workspaceName = this.getId().getWorkspaceId().getName();
    final var artifactName = this.getId().getName();

    final var artifactIri = this.httpConfig.getArtifactUri(workspaceName, artifactName);


    final var signifierIRI = artifactIri + "#" + actionName + "-Signifier";


    final var signifier = ResourceProfileGraphReader.readFromString(storeResponse).getExposedSignifiers().stream()
      .filter(sig -> sig.getIRIAsString().isPresent())
      .filter(sig -> sig.getIRIAsString().get().equals(signifierIRI))
      .findFirst();
    if (signifier.isPresent() && signifier.get().getActionSpecification().getOutputSpecification().isPresent()) {
      final var output = signifier.get().getActionSpecification().getOutputSpecification().get();
      final QualifiedValueSpecification qualifiedValueSpecification = (QualifiedValueSpecification) output;
      return getLengthOfQualifiedValueSpecificationList(qualifiedValueSpecification);
    }
    return 0;
  }

  private int getLengthOfQualifiedValueSpecificationList(final QualifiedValueSpecification qualifiedValueSpecification) {
    final var temp = qualifiedValueSpecification.getPropertySpecifications();
    int lengthOfList = 0;

    while (!temp.isEmpty()) {
      lengthOfList++;
      temp.remove(RDF.FIRST.stringValue());
      final var rest = temp.remove(RDF.REST.stringValue());
      if (rest instanceof QualifiedValueSpecification) {
        temp.putAll(((QualifiedValueSpecification) rest).getPropertySpecifications());
      }
    }
    return lengthOfList;
  }

}
