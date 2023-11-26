package org.hyperagents.yggdrasil.store;

import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import io.vertx.core.eventbus.ReplyException;
import org.apache.http.HttpStatus;
import org.eclipse.rdf4j.model.util.Models;
import org.junit.jupiter.api.Assertions;

public final class RdfStoreVerticleTestHelpers {
  private static final String REPRESENTATIONS_EQUAL_MESSAGE = "The representations must be equal";

  private RdfStoreVerticleTestHelpers() {}

  public static void assertEqualsThingDescriptions(final String expected, final String actual) {
    Assertions.assertTrue(
        Models.isomorphic(
          TDGraphReader.readFromString(ThingDescription.TDFormat.RDF_TURTLE, expected)
                       .getGraph()
                       .orElseThrow(),
          TDGraphReader.readFromString(ThingDescription.TDFormat.RDF_TURTLE, actual)
                       .getGraph()
                       .orElseThrow()
        ),
        REPRESENTATIONS_EQUAL_MESSAGE
    );
  }

  public static void assertNotFound(final Throwable t) {
    Assertions.assertEquals(
      HttpStatus.SC_NOT_FOUND,
      ((ReplyException) t).failureCode(),
      "Status code should be NOT FOUND"
    );
    Assertions.assertEquals(
      "Entity not found.",
      t.getMessage(),
      "The messages should be the same"
    );
  }

  public static void assertInternalServerError(final Throwable t) {
    Assertions.assertEquals(
        HttpStatus.SC_INTERNAL_SERVER_ERROR,
        ((ReplyException) t).failureCode(),
        "Status code should be INTERNAL SERVER ERROR"
    );
    Assertions.assertEquals(
        "Store request failed.",
        t.getMessage(),
        "The messages should be the same"
    );
  }
}
