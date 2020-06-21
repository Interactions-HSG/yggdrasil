package org.hyperagents.yggdrasil.cartago;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import cartago.Artifact;
import cartago.ArtifactId;
import cartago.CartagoException;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;

public abstract class HypermediaArtifact extends Artifact {
  private Map<String, List<ActionAffordance>> actionAffordances = 
      new HashMap<String, List<ActionAffordance>>();
  
  /**
   * Retrieves a hypermedia description of the artifact's interface. Current implementation is based
   * on the W3C Web of Things Thing Description: https://www.w3.org/TR/wot-thing-description/
   * 
   * @return An RDF description of the artifact and its interface.
   */
  public String getHypermediaDescription() {
    ThingDescription.Builder tdBuilder = new ThingDescription.Builder(getArtifactName())
        .addSemanticType("http://w3id.org/eve#Artifact")
        .addSemanticType(getSemanticType())
        .addThingURI(getArtifactUri());
    
    for (String actionName : actionAffordances.keySet()) {
      for (ActionAffordance action : actionAffordances.get(actionName)) {
        tdBuilder.addAction(action);
      }
    }
    
    String td = new TDGraphWriter(tdBuilder.build())
        .setNamespace("td", "https://www.w3.org/2019/wot/td#")
        .setNamespace("htv", "http://www.w3.org/2011/http#")
        .setNamespace("hctl", "https://www.w3.org/2019/wot/hypermedia#")
        .setNamespace("wotsec", "https://www.w3.org/2019/wot/security#")
        .setNamespace("dct", "http://purl.org/dc/terms/")
        .setNamespace("js", "https://www.w3.org/2019/wot/json-schema#")
        .setNamespace("eve", "http://w3id.org/eve#")
        .write();
    
    return td;
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
    // TODO: do not hard code the URI
    return "http://localhost:8080/artifacts/" + getArtifactName();
  }
  
  protected final void registerActionAffordance(String actionName, String relativeUri, 
      DataSchema inputSchema) {
    registerActionAffordance(actionName, "POST", relativeUri, inputSchema);
  }
  
  protected final void registerActionAffordance(String actionName, String methodName, 
      String relativeUri, DataSchema inputSchema) {
    ActionAffordance action = new ActionAffordance.Builder(
            new Form.Builder(getArtifactUri() + relativeUri)
              .setMethodName(methodName)
              .build())
        .addTitle("inc")
        .addInputSchema(inputSchema)
        .build();
    
    registerActionAffordance(actionName, action);
  }
  
  protected final void registerActionAffordance(String actionName, ActionAffordance action) {
    List<ActionAffordance> actions = actionAffordances.getOrDefault(actionName, 
        new ArrayList<ActionAffordance>());
    
    actions.add(action);
    actionAffordances.put(actionName, actions);
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
