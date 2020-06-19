package org.hyperagents.yggdrasil.cartago;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cartago.Artifact;
import cartago.ArtifactId;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class HypermediaArtifact extends Artifact {
  private static final Logger LOGGER = LoggerFactory.getLogger(HypermediaArtifact.class.getName());
  
  private Map<String, List<ActionAffordance>> actionAffordances = 
      new HashMap<String, List<ActionAffordance>>();
  
//  public HypermediaArtifact() {
//    ArtifactTemplateRegistry.register(this);
//  }
  
  /**
   * Retrieves the semantic type of an artifact.
   * 
   * @return A string denoting a semantic type (e.g., an IRI).
   */
  public abstract String getSemanticType();
  
  /**
   * Retrieves a hypermedia description of the artifact's interface. Current implementation is based
   * on the W3C Web of Things Thing Description: https://www.w3.org/TR/wot-thing-description/
   * 
   * @return An RDF description of the artifact and its interface.
   */
  public String getHypermediaDescription() {
    ThingDescription.Builder tdBuilder = new ThingDescription.Builder(getArtifactName())
        .addSemanticType("http://w3id.org/eve#Artifact")
        .addThingURI(getArtifactUri());
    
    for (String actionName : actionAffordances.keySet()) {
      for (ActionAffordance action : actionAffordances.get(actionName)) {
        tdBuilder.addAction(action);
      }
    }
    
    String td = TDGraphWriter.write(tdBuilder.build());
    
    LOGGER.info("Written TD: " + td);
    
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
  
  public Map<String, List<ActionAffordance>> getActionAffordances() {
    return actionAffordances;
  }
  
  protected abstract void collectActionAffordances();
  
  protected String getArtifactName() {
    return this.getId().getName();
  }
  
  protected String getArtifactUri() {
    // TODO: do not hard code the URI
    return "http://localhost:8080/artifacts/" + getArtifactName();
  }
  
  protected final void exposeActionAffordance(String methodName, ActionAffordance action) {
    List<ActionAffordance> actions = actionAffordances.getOrDefault(methodName, 
        new ArrayList<ActionAffordance>());
    
    actions.add(action);
    actionAffordances.put(methodName, actions);
    
    LOGGER.info("exposed affordances: " + actionAffordances);
  }
}
