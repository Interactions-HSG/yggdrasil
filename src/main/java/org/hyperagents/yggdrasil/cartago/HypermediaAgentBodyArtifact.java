package org.hyperagents.yggdrasil.cartago;

import cartago.*;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;

public class HypermediaAgentBodyArtifact extends HypermediaArtifact {

  private AgentId agentId;

  private Workspace workspace;

  private EventManager eventManager;



  public void init(Workspace workspace){
    this.agentId = this.getCreatorId();
    this.workspace = workspace;
    this.eventManager = new EventManager();

  }

  @OPERATION
  public void focus(String artifactName){
    ArtifactId artifactId = workspace.getArtifact(artifactName);
    focus(artifactId);
  }



  public void focus(ArtifactId artifactId){
    focus(artifactId, null);
  }


  public void focus(ArtifactId artifactId, IEventFilter filter ){
    boolean b = workspace.isArtifactPresent(artifactId.getName());
    if (b){
      ICartagoCallback callback = new EventManagerCallback(eventManager);
      try {
        workspace.focus(agentId, filter, callback, artifactId);
      } catch(CartagoException e){
        e.printStackTrace();
      }
    }
  }

  @OPERATION
  public void focusWhenAvailable(String artifactName){
    focusWhenAvailable(artifactName, null);
  }

  public void focusWhenAvailable(String artifactName, IEventFilter filter){
    try {
      ArtifactId aid = workspace.getArtifact(artifactName);
      while (aid == null) {
        await("artifactAvailable", artifactName);
        aid = workspace.getArtifact(artifactName);
      }
      ICartagoCallback callback = new EventManagerCallback(eventManager);
      workspace.focus(agentId, filter, callback, aid);
    } catch(Exception e){
      failed("Artifact Not Available.");
    }
  }

  @OPERATION
  public void stopFocus(String artifactName){
    ArtifactId artifactId = workspace.getArtifact(artifactName);
    ICartagoCallback callback = new EventManagerCallback(eventManager);
    try {
      workspace.stopFocus(agentId, callback, artifactId );

    } catch(Exception e){
      failed("Artifact Not Available.");
    }

  }


  @Override
  protected void registerInteractionAffordances() {
    DataSchema inputSchema = new ArraySchema.Builder()
      .addItem(new StringSchema.Builder().build())
      .build();
    Form focusForm = new Form.Builder(this.getArtifactUri()+"/focus")
      .setMethodName("PUT")
      .build();
    ActionAffordance focusAffordance = new ActionAffordance.Builder("focus", focusForm)
      .addInputSchema(inputSchema)
      .build();
    registerActionAffordance("http://example.org/focus", focusAffordance);
    Form focusWhenAvailableForm = new Form.Builder(this.getArtifactUri()+"/focusWhenAvailable")
      .setMethodName("PUT")
      .build();
    ActionAffordance focusWhenAvailableAffordance = new ActionAffordance.Builder("focusWhenAvailable", focusWhenAvailableForm)
      .addInputSchema(inputSchema)
      .build();
    registerActionAffordance("http://example.org/focusWhenAvailable", focusWhenAvailableAffordance);
    Form stopFocusForm = new Form.Builder(this.getArtifactUri()+"/stopFocus")
      .setMethodName("DELETE")
      .build();
    ActionAffordance stopFocusAffordance = new ActionAffordance.Builder("stopFocus", stopFocusForm)
      .addInputSchema(inputSchema)
      .build();
    registerActionAffordance("http://example.org/stopFocus", stopFocusAffordance);

  }
}
