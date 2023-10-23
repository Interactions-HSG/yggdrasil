package org.hyperagents.yggdrasil.cartago;

import cartago.Artifact;
import cartago.ArtifactId;
import cartago.CartagoException;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.security.NoSecurityScheme;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;

import java.util.*;

public abstract class HypermediaArtifact extends Artifact {
  private final Map<String, List<ActionAffordance>> actionAffordances = new HashMap<>();
  private final Model metadata = new LinkedHashModel();
  private SecurityScheme securityScheme = new NoSecurityScheme();

  /**
   * Retrieves a hypermedia description of the artifact's interface. Current implementation is based
   * on the W3C Web of Things <a href="https://www.w3.org/TR/wot-thing-description/">Thing Description</a>.
   *
   * @return An RDF description of the artifact and its interface.
   */
  public String getHypermediaDescription() {
    final var tdBuilder = new ThingDescription.Builder(this.getArtifactName())
                                              .addSecurityScheme(this.securityScheme)
                                              .addSemanticType("http://w3id.org/eve#Artifact")
                                              .addSemanticType(this.getSemanticType())
                                              .addThingURI(this.getArtifactUri())
                                              .addGraph(this.metadata);
    this.actionAffordances.values().stream().flatMap(List::stream).forEach(tdBuilder::addAction);

    return new TDGraphWriter(tdBuilder.build()).setNamespace("td", "https://www.w3.org/2019/wot/td#")
                                               .setNamespace("htv", "http://www.w3.org/2011/http#")
                                               .setNamespace("hctl", "https://www.w3.org/2019/wot/hypermedia#")
                                               .setNamespace("wotsec", "https://www.w3.org/2019/wot/security#")
                                               .setNamespace("dct", "http://purl.org/dc/terms/")
                                               .setNamespace("js", "https://www.w3.org/2019/wot/json-schema#")
                                               .setNamespace("eve", "http://w3id.org/eve#")
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
    return HypermediaArtifactRegistry.getInstance().getHttpArtifactsPrefix(this.getId().getWorkspaceId().getName())
           + this.getArtifactName();
  }

  protected final void registerActionAffordance(final String actionClass, final String actionName, final String relativeUri) {
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
      Optional.ofNullable(inputSchema).map(actionBuilder::addInputSchema).orElse(actionBuilder).build()
    );
  }

  protected final void registerActionAffordance(final String actionName, final ActionAffordance action) {
    final var actions = this.actionAffordances.getOrDefault(actionName, new ArrayList<>());

    actions.add(action);
    this.actionAffordances.put(actionName, actions);
  }

  protected final void setSecurityScheme(final SecurityScheme scheme) {
    this.securityScheme = scheme;
  }

  protected final void addMetadata(final Model model) {
    this.metadata.addAll(model);
  }

  Map<String, List<ActionAffordance>> getActionAffordances() {
    return this.actionAffordances;
  }

  private String getSemanticType() {
    return HypermediaArtifactRegistry.getInstance()
                                     .getArtifactSemanticType(this.getClass().getCanonicalName())
                                     .orElseThrow(() -> new RuntimeException("Artifact was not registered!"));
  }
}
