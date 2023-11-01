package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import ch.unisg.ics.interactions.wot.td.ThingDescription.TDFormat;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpRequest;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.IntegerSchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.security.APIKeySecurityScheme;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;
import ch.unisg.ics.interactions.wot.td.vocabularies.WoTSec;
import java.io.IOException;
import java.util.HashMap;
import java.util.NoSuchElementException;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifact;

public class SpatialCalculator2D extends HypermediaArtifact {
  private static final String PREFIX = "http://example.org/";
  private static final String SET_BASE_ACTION = PREFIX + "SetBase";

  private String robotUri;
  private String apiKey;

  private int xr;
  private int yr;

  public void init(final String robotUri, final String apiKey, final int xr, final int yr) {
    this.xr = xr;
    this.yr = yr;

    this.robotUri = robotUri;
    this.apiKey = apiKey;
  }

  @OPERATION
  public void moveTo(final int x, final int y) {
    final var degrees = this.angularDisplacement(x, y);
    final var digital = this.angularToDigital(degrees);

    try {
      final var td = TDGraphReader.readFromURL(TDFormat.RDF_TURTLE, robotUri);
      final var action = td.getFirstActionBySemanticType(SET_BASE_ACTION).orElseThrow();

      final var payload = new HashMap<String, Object>();
      payload.put(PREFIX + "BaseJoint", Math.round(digital));

      final var response =
          new TDHttpRequest(action.getFirstForm().orElseThrow(), TD.invokeAction)
            .setAPIKey(
              (APIKeySecurityScheme) td.getFirstSecuritySchemeByType(WoTSec.APIKeySecurityScheme)
                                       .orElseThrow(),
              this.apiKey
            )
            .setObjectPayload((ObjectSchema) action.getInputSchema().orElseThrow(), payload)
            .execute();

      // Match any 2XX status code
      if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
        this.failed("Robot request failed with status code: " + response.getStatusCode());
      }
    } catch (final IOException | NoSuchElementException e) {
      this.failed(e.getMessage());
    }
  }

  private double angularToDigital(final double degrees) {
    final var dist = 1023.0 / 360.0;
    //1023/360
    return dist * (360.0 - degrees);
  }

  //computes the degrees of clock-wise rotation for transfering an axis from the line
  // L{R(xr,yr),P1(0,y)} to the line L{R(xr,yr) and P2(x,y)}
  double angularDisplacement(final int x, final int y) {
    final var rad = Math.atan2((y - yr), (x - xr));
    final var deg = rad * (180.0 / Math.PI);

    return deg >= 0.0 ? deg : 360.0 + deg;
  }

  @Override
  protected void registerInteractionAffordances() {
    // Register one action affordance with an input schema
    this.registerActionAffordance(
        "http://example.org#MoveTo",
        "moveTo",
        "/moveTo",
        new ArraySchema.Builder()
                       .addSemanticType(PREFIX + "2DCoordinates")
                       .addItem(new IntegerSchema.Builder().build())
                       .addMinItems(2)
                       .addMaxItems(2)
                       .build()
    );
  }
}
