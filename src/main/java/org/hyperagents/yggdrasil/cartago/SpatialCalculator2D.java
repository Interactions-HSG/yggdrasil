package org.hyperagents.yggdrasil.cartago;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import cartago.OPERATION;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.ThingDescription.TDFormat;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpRequest;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpResponse;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.IntegerSchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.security.APIKeySecurityScheme;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;
import ch.unisg.ics.interactions.wot.td.vocabularies.WoTSec;

public class SpatialCalculator2D extends HypermediaArtifact {
  private static final String PREFIX = "http://example.org/";
  private static final String SET_BASE_ACTION = PREFIX + "SetBase";
  
  private String robotUri;
  private String apiKey;
  
  private int xr;
  private int yr;
  
  public void init(String robotUri, String apiKey, int xr, int yr) {
    this.xr = xr;
    this.yr = yr;
    
    this.robotUri = robotUri;
    this.apiKey = apiKey;
  }
  
  @OPERATION
  public void moveTo(int x, int y) {
    double degrees = angularDisplacement(x, y);
    double digital = angularToDigital(degrees);
    
    try {
      ThingDescription td = TDGraphReader.readFromURL(TDFormat.RDF_TURTLE, robotUri);
      ActionAffordance action = td.getFirstActionBySemanticType(SET_BASE_ACTION).get();
      
      Map<String, Object> payload = new HashMap<String, Object>();
      payload.put(PREFIX + "BaseJoint", Math.round(digital));
      
      TDHttpResponse response = new TDHttpRequest(action.getFirstForm().get(), TD.invokeAction)
          .setAPIKey((APIKeySecurityScheme) td.getFirstSecuritySchemeByType(WoTSec.APIKeySecurityScheme)
              .get(), apiKey)
          .setObjectPayload((ObjectSchema) action.getInputSchema().get(), payload)
          .execute();
      
      // Match any 2XX status code
      if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
        failed("Robot request failed with status code: " + response.getStatusCode());
      }
    } catch (IOException | NoSuchElementException e) {
      failed(e.getMessage());
    } 
  }
  
  private double angularToDigital(double degrees){
    double dist = 1023.0/360.0;
    //1023/360
    return dist * (360.0-degrees);
  }
  
  //computes the degrees of clock-wise rotation for transfering an axis from the line 
  // L{R(xr,yr),P1(0,y)} to the line L{R(xr,yr) and P2(x,y)}
  double angularDisplacement(int x, int y){
    double rad = Math.atan2((y - yr), (x - xr));
    double deg = rad * (180.0 / Math.PI);
    
    return deg >= 0.0 ? deg : 360.0 + deg;
  }
  
  @Override
  protected void registerInteractionAffordances() {
    // Register one action affordance with an input schema
    registerActionAffordance("http://example.org#MoveTo", "moveTo", "/moveTo", 
            new ArraySchema.Builder().addSemanticType(PREFIX + "2DCoordinates")
              .addItem(new IntegerSchema.Builder().build())
              .addMinItems(2)
              .addMaxItems(2)
              .build());
  }
}
