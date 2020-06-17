package org.hyperagents.yggdrasil.core;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hyperagents.yggdrasil.cartago.HypermediaArtifact;

import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class HypermediaArtifactRegistry {
  private static final Logger LOGGER = LoggerFactory.getLogger(HypermediaArtifactRegistry.class
      .getName());
  
  private static HypermediaArtifactRegistry registry;
  
  private final Map<String, String> artifactSemanticTypes;
  private final Map<String, String> artifactDescriptions;
  private final Map<String, String> artifactActionRouter;
  
  private HypermediaArtifactRegistry() {
    artifactSemanticTypes = new Hashtable<String, String>();
    artifactDescriptions = new Hashtable<String, String>();
    artifactActionRouter = new Hashtable<String, String>();
  }
  
  public static synchronized HypermediaArtifactRegistry getInstance() {
    if (registry == null) {
        registry = new HypermediaArtifactRegistry();
    }
    
    return registry;
  }
  
  public void register(HypermediaArtifact artifact) {
    String artifactName = artifact.getArtifactId().getName();
    
    artifactSemanticTypes.put(artifactName, artifact.getSemanticType());
    artifactDescriptions.put(artifactName, artifact.getHypermediaDescription());
    
    Map<String, List<ActionAffordance>> actions = artifact.getActionAffordances();
    LOGGER.info("Actions: #" + actions.size());
    
    for (String actionName : actions.keySet()) {
      for (ActionAffordance action : actions.get(actionName)) {
        Optional<Form> form = action.getFirstForm();
        
        if (form.isPresent()) {
          artifactActionRouter.put(form.get().getMethodName().get() + form.get().getTarget(), 
              actionName);
        }
      }
    }
    
    LOGGER.info("Action router:" + artifactActionRouter);
  }
  
  public String getArtifactDescription(String artifactName) {
    return artifactDescriptions.get(artifactName);
  }
  
  public String getActionName(String method, String requestURI) {
    return artifactActionRouter.get(method + requestURI);
  }
}
