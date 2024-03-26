package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.Artifact;
import cartago.ArtifactId;
import cartago.CartagoException;
import ch.unisg.ics.interactions.hmas.interaction.shapes.IOSpecification;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.ActionSpecification;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.Form;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.Signifier;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import io.vertx.core.json.JsonObject;
import java.net.URI;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifactHMASRegistry;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.RepresentationFactory;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.RepresentationFactoryHMASImpl;

public abstract class HypermediaHMASArtifact extends Artifact {
  private final ListMultimap<String, Signifier> signifiers =
      Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);
  private final Model metadata = new LinkedHashModel();
  private final Set<String> feedbackActions = new HashSet<>();
  private final Map<String, UnaryOperator<Object>> responseConverterMap = new HashMap<>();
  private HttpInterfaceConfig httpConfig = new HttpInterfaceConfigImpl(JsonObject.of());
  private RepresentationFactory representationFactory =
      new RepresentationFactoryHMASImpl(this.httpConfig);


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
      HypermediaArtifactHMASRegistry.getInstance()
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
      this.representationFactory = new RepresentationFactoryHMASImpl(this.httpConfig);
    }
    this.registerInteractionAffordances();
    HypermediaArtifactHMASRegistry.getInstance().register(this);
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
    this.registerSignifier(actionClass, actionName, "POST", relativeUri, inputSchema);
  }

  protected final void registerSignifier(
      final String actionClass,
      final String actionName,
      final String methodName,
      final String relativeUri,
      final IOSpecification inputSchema
  ) {


    // TODO: set content type dynamically
    // TODO: set input correctly
    final var form = new Form.Builder(this.getArtifactUri() + relativeUri)
      .setIRIAsString(this.getArtifactUri() + "#" + actionName)
      .setMethodName(methodName)
      .setContentType("application/json")
      .build();

    final var actionSpecification = new ActionSpecification.Builder(form).setRequiredSemanticTypes(Collections.singleton(actionClass)).addSemanticType(actionClass);

    if (inputSchema != null) {
      actionSpecification.setInputSpecification(inputSchema);
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
