package org.hyperagents.yggdrasil.cartago.artifacts;

import java.io.IOException;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;

import cartago.OPERATION;
import ch.unisg.ics.interactions.wot.td.schemas.NumberSchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.security.APIKeySecurityScheme;
import ch.unisg.ics.interactions.wot.td.security.APIKeySecurityScheme.TokenLocation;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifact;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifactRegistry;

public class PhantomX3D extends HypermediaArtifact {
  private static final String PREFIX = "https://ci.mines-stetienne.fr/kg/ontology#";

  private static final String SET_BASE_URI = "/base";
  private static final String SET_GRIPPER_URI = "/gripper";
  private static final String SET_WRIST_ANGLE_URI = "/wrist/angle";
  private static final String SET_SHOULDER_URI = "/shoulder";
  private static final String RESET_URI = "/reset";

  private static final int SHORT_WAIT_TIME = 1000;
  private static final int WAIT_TIME = 3000;

  private enum State {
    NEUTRAL,
    PIKCUP_LOCATION,
    PLACE_LOCATION,
    IN_TRANSIT
  }

  private String robotBaseUri;
  private State state;

  public void init(String robotBaseUri) {
    this.state = State.NEUTRAL;
    this.robotBaseUri = robotBaseUri;
  }

  @OPERATION
  public void grasp() {
    this.await_time(SHORT_WAIT_TIME);
    invokeAction(SET_GRIPPER_URI, 400);
  }

  @OPERATION
  public void release() {
    this.await_time(SHORT_WAIT_TIME);
    invokeAction(SET_GRIPPER_URI, 512);
  }

  @OPERATION
  public void reset() {
    this.await_time(SHORT_WAIT_TIME);
    invokeAction(RESET_URI, null);
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

    // Add initial coordinates, these are currently hard-coded
    ModelBuilder builder = new ModelBuilder();
    ValueFactory rdf = SimpleValueFactory.getInstance();

    BNode coordinates = rdf.createBNode();
    builder.add(getArtifactUri(), rdf.createIRI(PREFIX + "hasOriginCoordinates"), coordinates);
    builder.add(coordinates, rdf.createIRI(PREFIX + "coordX"), rdf.createLiteral(2.7));
    builder.add(coordinates, rdf.createIRI(PREFIX + "coordY"), rdf.createLiteral(-0.5));
    builder.add(coordinates, rdf.createIRI(PREFIX + "coordZ"), rdf.createLiteral(0.8));

    addMetadata(builder.build());

    setSecurityScheme(new APIKeySecurityScheme(TokenLocation.HEADER, "X-API-Key"));
  }

  private void moveToNeural() {
    state = State.IN_TRANSIT;
    this.await_time(1000);
    invokeAction(RESET_URI, null);
    state = State.NEUTRAL;
  }

  private void moveToPickUpLocationFromNeural() {
    state = State.IN_TRANSIT;

    this.await_time(SHORT_WAIT_TIME);
    invokeAction(SET_GRIPPER_URI, 512);
    this.await_time(WAIT_TIME);
    invokeAction(SET_BASE_URI, 512);
    this.await_time(WAIT_TIME);
    invokeAction(SET_WRIST_ANGLE_URI, 390);
    this.await_time(WAIT_TIME);
    invokeAction(SET_SHOULDER_URI, 510);

    state = State.PIKCUP_LOCATION;
  }

  private void moveToPlaceLocationFromPickup() {
    state = State.IN_TRANSIT;

    this.await_time(WAIT_TIME);
    invokeAction(SET_SHOULDER_URI, 400);
    this.await_time(WAIT_TIME);
    invokeAction(SET_BASE_URI, 256);
    this.await_time(WAIT_TIME);
    invokeAction(SET_SHOULDER_URI, 510);
    this.await_time(WAIT_TIME);

    state = State.PLACE_LOCATION;
  }

  private void invokeAction(String relativeUri, Integer value) {
    HttpClient client = HttpClients.createDefault();

    ClassicHttpRequest request = new BasicClassicHttpRequest("PUT", robotBaseUri + relativeUri);

    String apiKey = HypermediaArtifactRegistry.getInstance().getAPIKeyForArtifact(getArtifactUri());
    request.setHeader("X-API-Key", apiKey);

    request.setEntity(new StringEntity("{\"value\" : " + value + "}",
        ContentType.create("application/json")));

    try {
      client.execute(request);
    } catch (IOException e) {
      failed(e.getMessage());
    }
  }
}
