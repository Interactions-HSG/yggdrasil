package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import ch.unisg.ics.interactions.wot.td.schemas.NumberSchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;

public class PhantomX3D extends HypermediaTDArtifact {
  private static final String PREFIX = "https://ci.mines-stetienne.fr/kg/ontology#";

  private static final String SET_BASE_URI = "/base";
  private static final String SET_GRIPPER_URI = "/gripper";
  private static final String SET_WRIST_ANGLE_URI = "/wrist/angle";
  private static final String SET_SHOULDER_URI = "/shoulder";
  private static final String RESET_URI = "/reset";

  private static final int SHORT_WAIT_TIME = 1000;
  private static final int WAIT_TIME = 3000;

  @SuppressFBWarnings("PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_CLASS_NAMES")
  private enum State {
    NEUTRAL,
    PIKCUP_LOCATION,
    PLACE_LOCATION,
    IN_TRANSIT
  }

  private String robotBaseUri;
  private State state;


  public void init(final String robotBaseUri) {
    this.state = State.NEUTRAL;
    this.robotBaseUri = robotBaseUri;
  }

  @OPERATION
  public void grasp() {
    this.await_time(SHORT_WAIT_TIME);
    this.invokeAction(SET_GRIPPER_URI, 400);
  }

  @OPERATION
  public void release() {
    this.await_time(SHORT_WAIT_TIME);
    this.invokeAction(SET_GRIPPER_URI, 512);
  }

  @OPERATION
  public void reset() {
    this.await_time(SHORT_WAIT_TIME);
    this.invokeAction(RESET_URI, null);
    this.state = State.NEUTRAL;
  }

  @OPERATION
  public void moveTo() {
    if (this.state == State.IN_TRANSIT) {
      this.failed("Illegal this.state: cannot move, robot is in transit");
    } else if (this.state == State.NEUTRAL) {
      this.moveToPickUpLocationFromNeural();
    } else if (this.state == State.PIKCUP_LOCATION) {
      moveToPlaceLocationFromPickup();
    } else if (this.state == State.PLACE_LOCATION) {
      this.moveToNeural();
      this.moveToPickUpLocationFromNeural();
    }
  }

  @Override
  protected void registerInteractionAffordances() {
    this.registerActionAffordance(
        PREFIX + "MoveTo",
        "moveTo",
        "moveTo",
      new ObjectSchema.Builder()
        .addSemanticType(PREFIX + "FactoryFloorPosition")
        .addProperty(
          "x",
          new NumberSchema.Builder()
            .addSemanticType(PREFIX + "XCoordinate")
            .build()
        )
        .addProperty(
          "y",
          new NumberSchema.Builder()
            .addSemanticType(PREFIX + "YCoordinate")
            .build()
        )
        .addProperty(
          "z",
          new NumberSchema.Builder()
            .addSemanticType(PREFIX + "ZCoordinate")
            .build()
        )
        .build()
    );
    this.registerActionAffordance(PREFIX + "Grasp", "grasp", "grasp");
    this.registerActionAffordance(PREFIX + "Release", "release", "release");
    this.registerActionAffordance(PREFIX + "Reset", "reset", "reset");

    // Add initial coordinates, these are currently hard-coded
    final var builder = new ModelBuilder();
    final var rdf = SimpleValueFactory.getInstance();

    final var coordinates = rdf.createBNode();
    builder.add(getArtifactUri(), rdf.createIRI(PREFIX + "hasOriginCoordinates"), coordinates);
    builder.add(coordinates, rdf.createIRI(PREFIX + "coordX"), rdf.createLiteral(2.7));
    builder.add(coordinates, rdf.createIRI(PREFIX + "coordY"), rdf.createLiteral(-0.5));
    builder.add(coordinates, rdf.createIRI(PREFIX + "coordZ"), rdf.createLiteral(0.8));

    this.addMetadata(builder.build());

  }


  private void moveToNeural() {
    this.state = State.IN_TRANSIT;
    this.await_time(1000);
    this.invokeAction(RESET_URI, null);
    this.state = State.NEUTRAL;
  }

  private void moveToPickUpLocationFromNeural() {
    this.state = State.IN_TRANSIT;

    this.await_time(SHORT_WAIT_TIME);
    this.invokeAction(SET_GRIPPER_URI, 512);
    this.await_time(WAIT_TIME);
    this.invokeAction(SET_BASE_URI, 512);
    this.await_time(WAIT_TIME);
    this.invokeAction(SET_WRIST_ANGLE_URI, 390);
    this.await_time(WAIT_TIME);
    this.invokeAction(SET_SHOULDER_URI, 510);

    this.state = State.PIKCUP_LOCATION;
  }

  private void moveToPlaceLocationFromPickup() {
    this.state = State.IN_TRANSIT;

    this.await_time(WAIT_TIME);
    this.invokeAction(SET_SHOULDER_URI, 400);
    this.await_time(WAIT_TIME);
    this.invokeAction(SET_BASE_URI, 256);
    this.await_time(WAIT_TIME);
    this.invokeAction(SET_SHOULDER_URI, 510);
    this.await_time(WAIT_TIME);

    this.state = State.PLACE_LOCATION;
  }

  private void invokeAction(final String relativeUri, final Integer value) {
    try (var client = HttpClients.createDefault()) {

      final var request =
          new BasicClassicHttpRequest("PUT", robotBaseUri + relativeUri);

      final var apiKey = getApiKey();
      request.setHeader("X-API-Key", apiKey);

      request.setEntity(
          new StringEntity("{\"value\" : " + value + "}",
          ContentType.create("application/json"))
      );

      client.execute(request, response -> null);
    } catch (final IOException e) {
      this.failed(e.getMessage());
    }
  }
}
