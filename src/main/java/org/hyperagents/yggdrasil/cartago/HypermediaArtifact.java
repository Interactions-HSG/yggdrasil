package org.hyperagents.yggdrasil.cartago;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;

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

public abstract class HypermediaArtifact extends Artifact {
  private final Map<String, List<ActionAffordance>> actionAffordances = new HashMap<>();

  private SecurityScheme securityScheme = new NoSecurityScheme();
  private final Model metadata = new LinkedHashModel();

  /**
   * Retrieves a hypermedia description of the artifact's interface. Current implementation is based
   * on the W3C Web of Things Thing Description: https://www.w3.org/TR/wot-thing-description/
   *
   * @return An RDF description of the artifact and its interface.
   */
  public String getHypermediaDescription() {
    ThingDescription.Builder tdBuilder = new ThingDescription.Builder(getArtifactName())
        .addSecurityScheme(securityScheme)
        .addSemanticType("http://w3id.org/eve#Artifact")
        .addSemanticType(getSemanticType())
        .addThingURI(getArtifactUri())
        .addGraph(metadata);

    for (String actionName : actionAffordances.keySet()) {
      for (ActionAffordance action : actionAffordances.get(actionName)) {
        tdBuilder.addAction(action);
      }
    }

    return new TDGraphWriter(tdBuilder.build())
        .setNamespace("td", "https://www.w3.org/2019/wot/td#")
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

    registerInteractionAffordances();
    HypermediaArtifactRegistry.getInstance().register(this);
  }

  protected abstract void registerInteractionAffordances();

  protected String getArtifactName() {
    return this.getId().getName();
  }

  protected String getArtifactUri() {
    String wkspName = getId().getWorkspaceId().getName();

    return HypermediaArtifactRegistry.getInstance().getHttpArtifactsPrefix(wkspName)
        + getArtifactName();
  }

  protected final void registerActionAffordance(String actionClass, String actionName,
      String relativeUri) {
    registerActionAffordance(actionClass, actionName, relativeUri, null);
  }

  protected final void registerActionAffordance(String actionClass, String actionName,
      String relativeUri, DataSchema inputSchema) {
    registerActionAffordance(actionClass, actionName, "POST", relativeUri, inputSchema);
  }

  protected final void registerActionAffordance(String actionClass, String actionName,
      String methodName, String relativeUri, DataSchema inputSchema) {
    ActionAffordance.Builder actionBuilder = new ActionAffordance.Builder(actionName,
            new Form.Builder(getArtifactUri() + relativeUri)
              .setMethodName(methodName)
              .build())
        .addSemanticType(actionClass)
        .addTitle(actionName);

    if (inputSchema != null) {
      actionBuilder.addInputSchema(inputSchema);
    }

    registerActionAffordance(actionName, actionBuilder.build());
  }

  protected final void registerActionAffordance(String actionName, ActionAffordance action) {
    List<ActionAffordance> actions = actionAffordances.getOrDefault(actionName, new ArrayList<>());

    actions.add(action);
    actionAffordances.put(actionName, actions);
  }

  protected final void setSecurityScheme(SecurityScheme scheme) {
    this.securityScheme = scheme;
  }

  protected final void addMetadata(Model model) {
    this.metadata.addAll(model);
  }

  Map<String, List<ActionAffordance>> getActionAffordances() {
    return actionAffordances;
  }

  private String getSemanticType() {
    Optional<String> semType = HypermediaArtifactRegistry.getInstance().getArtifactSemanticType(
        this.getClass().getCanonicalName());

    if (semType.isPresent()) {
      return semType.get();
    }

    throw new RuntimeException("Artifact was not registered!");
  }
}
