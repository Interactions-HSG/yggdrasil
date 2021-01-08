package org.hyperagents.yggdrasil.cartago;

import cartago.OPERATION;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class Annotator extends HypermediaArtifact{
  private static final String PREFIX = "http://example.org/";

  private static final Logger LOGGER = LoggerFactory.getLogger(Annotator.class.getName());

  @OPERATION
  public void annotate(String uri, String text) {
    LOGGER.info("Annotate " + uri + " with text " + text);
  }

  @Override
  protected void registerInteractionAffordances() {
    registerActionAffordance(PREFIX +"CreateAnnotation", "annotate", "/annotations",
      new ObjectSchema.Builder()
        .addProperty("uri", new StringSchema.Builder()
          .addSemanticType(PREFIX + "Uri")
          .build())
        .addProperty("text", new StringSchema.Builder()
          .addSemanticType(PREFIX + "Text")
          .build())
        .addRequiredProperties("uri", "text")
        .build());
  }
}
