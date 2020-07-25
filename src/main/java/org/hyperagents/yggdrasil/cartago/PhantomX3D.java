package org.hyperagents.yggdrasil.cartago;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import cartago.OPERATION;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.ThingDescription.TDFormat;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpRequest;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpResponse;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.schemas.NumberSchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.security.APIKeySecurityScheme;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;
import ch.unisg.ics.interactions.wot.td.vocabularies.WoTSec;

public class PhantomX3D extends HypermediaArtifact {
  private static final String PREFIX = "https://ci.mines-stetienne.fr/kg/ontology#";
  private static final String SET_BASE = PREFIX + "SetBase";
  private static final String SET_GRIPPER = PREFIX + "SetGripper";
  private static final String SET_WRIST_ANGLE = PREFIX + "SetWristAngle";
  private static final String SET_SHOULDER = PREFIX + "SetShoulder";
  private static final String RESET = PREFIX + "Reset";
  
  private static final String INTEGER_SCHEMA = "https://www.w3.org/2019/wot/json-schema#IntegerSchema";
  
  private static final int SHORT_WAIT_TIME = 1000;
  private static final int WAIT_TIME = 3000;
  
  private enum State {
    NEUTRAL,
    PIKCUP_LOCATION,
    PLACE_LOCATION,
    IN_TRANSIT
  }
  
  private ThingDescription td;
  private State state;
  
  public void init(String robotUri) {
    this.state = State.NEUTRAL;
    
    try {
      td = TDGraphReader.readFromURL(TDFormat.RDF_TURTLE, robotUri);
    } catch (IOException e) {
      failed(e.getMessage());
    }
  }
  
  @OPERATION
  public void grasp() {
    this.await_time(SHORT_WAIT_TIME);
    invokeAction(SET_GRIPPER, 400);
  }
  
  @OPERATION
  public void release() {
    this.await_time(SHORT_WAIT_TIME);
    invokeAction(SET_GRIPPER, 512);
  }
  
  @OPERATION
  public void reset() {
    this.await_time(SHORT_WAIT_TIME);
    invokeAction(RESET, null);
    state = State.NEUTRAL;
  }
  
  @OPERATION
  public void moveTo() {
    if (state == State.IN_TRANSIT) {
      failed("Illegal state: cannot move, robot is in transit");
    } else if (state == State.NEUTRAL) {
      moveToPickUpLocationFromNeural();
    } else if (state == State.PIKCUP_LOCATION) {
      moveToPlaceLocationFromPickup();
    } else if (state == State.PLACE_LOCATION) {
      moveToNeural();
      moveToPickUpLocationFromNeural();
    }
  }
  
  @Override
  protected void registerInteractionAffordances() {
    registerActionAffordance(PREFIX + "MoveTo", "moveTo", "/moveTo", 
        new ObjectSchema.Builder().addSemanticType(PREFIX + "FactoryFloorPosition")
          .addProperty("x", new NumberSchema.Builder()
              .addSemanticType(PREFIX + "XCoordinate")
              .build())
          .addProperty("y", new NumberSchema.Builder()
              .addSemanticType(PREFIX + "YCoordinate")
              .build())
          .addProperty("z", new NumberSchema.Builder()
              .addSemanticType(PREFIX + "ZCoordinate")
              .build())
          .build());
    registerActionAffordance(PREFIX + "Grasp", "grasp", "/grasp");
    registerActionAffordance(PREFIX + "Release", "release", "/release");
    registerActionAffordance(PREFIX + "Reset", "reset", "/reset");
  }
  
  private void moveToNeural() {
    state = State.IN_TRANSIT;
    this.await_time(1000);
    invokeAction(RESET, null);
    state = State.NEUTRAL;
  }
  
  private void moveToPickUpLocationFromNeural() {
    state = State.IN_TRANSIT;
    
    this.await_time(WAIT_TIME);
    invokeAction(SET_BASE, 512);
    this.await_time(WAIT_TIME);
    invokeAction(SET_WRIST_ANGLE, 390);
    this.await_time(WAIT_TIME);
    invokeAction(SET_SHOULDER, 510);
    
    state = State.PIKCUP_LOCATION;
  }
  
  private void moveToPlaceLocationFromPickup() {
    state = State.IN_TRANSIT;
    
    this.await_time(WAIT_TIME);
    invokeAction(SET_SHOULDER, 400);
    this.await_time(WAIT_TIME);
    invokeAction(SET_BASE, 256);
    this.await_time(WAIT_TIME);
    invokeAction(SET_SHOULDER, 510);
    this.await_time(WAIT_TIME);
    
    state = State.PLACE_LOCATION;
  }
  
  private void invokeAction(String actionType, Integer value) {
    try {
      ActionAffordance action = td.getFirstActionBySemanticType(actionType).get();
      
      String apiKey = HypermediaArtifactRegistry.getInstance().getAPIKeyForArtifact(getArtifactUri());
      TDHttpRequest request = new TDHttpRequest(action.getFirstForm().get(), TD.invokeAction)
          .setAPIKey((APIKeySecurityScheme) td.getFirstSecuritySchemeByType(WoTSec.APIKeySecurityScheme)
              .get(), apiKey);
      
      if (value != null) {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put(INTEGER_SCHEMA, value);
        
        request.setObjectPayload((ObjectSchema) action.getInputSchema().get(), payload);
      }
      
      TDHttpResponse response = request.execute();
      
      // Match any 2XX status code
      if (response.getStatusCode() < 200 && response.getStatusCode() >= 300) {
        failed("Robot request failed with status code: " + response.getStatusCode());
      }
    } catch (Exception e) {
      failed(e.getMessage());
    }
  }
}
