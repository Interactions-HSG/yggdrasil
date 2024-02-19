package org.hyperagents.yggdrasil.store;

import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphReader;
import io.vertx.core.eventbus.ReplyException;
import org.apache.http.HttpStatus;
import org.eclipse.rdf4j.model.util.Models;
import org.junit.jupiter.api.Assertions;

public final class RdfStoreVerticleTestHelpers {
  private static final String REPRESENTATIONS_EQUAL_MESSAGE = "The representations must be equal";
  private static final String BAD_EXCEPTION_MESSAGE = "The exception was not of the right type";
  private static final String MESSAGES_EQUAL_MESSAGE = "The messages should be the same";

  private RdfStoreVerticleTestHelpers() {}

  public static void assertEqualsThingDescriptions(final String expected, final String actual) {
    Assertions.assertTrue(
        Models.isomorphic(
          ResourceProfileGraphReader.getModelFromString(expected),
          ResourceProfileGraphReader.getModelFromString(actual)
        ),
        REPRESENTATIONS_EQUAL_MESSAGE
    );
  }

  public static void assertNotFound(final Throwable t) {
    if (t instanceof ReplyException r) {
      Assertions.assertEquals(
          HttpStatus.SC_NOT_FOUND,
          r.failureCode(),
          "Status code should be NOT FOUND"
      );
    } else {
      Assertions.fail(BAD_EXCEPTION_MESSAGE);
    }
    Assertions.assertEquals(
        "Entity not found.",
        t.getMessage(),
        MESSAGES_EQUAL_MESSAGE
    );
  }

  public static void assertBadRequest(final Throwable t) {
    if (t instanceof ReplyException r) {
      Assertions.assertEquals(
          HttpStatus.SC_BAD_REQUEST,
          r.failureCode(),
          "Status code should be BAD REQUEST"
      );
    } else {
      Assertions.fail(BAD_EXCEPTION_MESSAGE);
    }
    Assertions.assertEquals(
        "Arguments badly formatted.",
        t.getMessage(),
        MESSAGES_EQUAL_MESSAGE
    );
  }
}
