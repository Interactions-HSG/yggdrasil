package org.hyperagents.yggdrasil.store;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@ExtendWith(VertxExtension.class)
public class RdfStoreVerticleQueryTest {
  private static final String PLATFORM_URI = "http://localhost:8080/";
  private static final String CONTENTS_EQUAL_MESSAGE = "The contents should be equal";
  public static final String THING_PARAM = "thing";
  public static final String NAME_PARAM = "name";

  private RdfStoreMessagebox messagebox;

  @BeforeEach
  public void setUp(final Vertx vertx, final VertxTestContext ctx) {
    this.messagebox = new RdfStoreMessagebox(vertx.eventBus());
    vertx.deployVerticle(new RdfStoreVerticle(), ctx.succeedingThenComplete());
  }

  @AfterEach
  public void tearDown(final Vertx vertx, final VertxTestContext ctx) {
    vertx.close(ctx.succeedingThenComplete());
  }

  @Test
  public void testQueryRequest(final VertxTestContext ctx) {
    this.messagebox
        .sendMessage(new RdfStoreMessage.QueryKnowledgeGraph(
            """
            PREFIX td: <https://www.w3.org/2019/wot/td#>
            PREFIX hmas: <https://purl.org/hmas/core/>
            SELECT ?thing ?name
            WHERE {
                ?thing td:hasActionAffordance [
                         td:name ?name
                       ];
                       a hmas:HypermediaMASPlatform.
            }
            """
        ))
        .onSuccess(r -> Assertions.assertEquals(
            JsonArray.of(JsonObject.of(THING_PARAM, PLATFORM_URI, NAME_PARAM, "createWorkspace")),
            Json.decodeValue(r.body()),
            CONTENTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testQueryRequestWithUnassignedBinding(final VertxTestContext ctx) {
    this.messagebox
        .sendMessage(new RdfStoreMessage.QueryKnowledgeGraph(
            """
            PREFIX td: <https://www.w3.org/2019/wot/td#>
            PREFIX hmas: <https://purl.org/hmas/core/>
            SELECT ?thing ?name
            WHERE {
                ?thing td:hasActionAffordance [
                         td:name "createWorkspace"
                       ];
                       a hmas:HypermediaMASPlatform.
            }
            """
        ))
        .onSuccess(r -> Assertions.assertEquals(
            JsonArray.of(JsonObject.of(THING_PARAM, PLATFORM_URI, NAME_PARAM, null)),
            Json.decodeValue(r.body()),
            CONTENTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testQueryRequestWithMalformedQuery(final VertxTestContext ctx) {
    this.messagebox
        .sendMessage(new RdfStoreMessage.QueryKnowledgeGraph(
            """
            PREFIX wot: <https://www.w3.org/2019/wot/td#>
            PREFIX eve: <http://w3id.org/eve#>
            SELECT ?thing ?name
            """
        ))
        .onFailure(t -> Assertions.assertEquals(
            HttpStatus.SC_INTERNAL_SERVER_ERROR,
            ((ReplyException) t).failureCode(),
            "The failure code should be INTERNAL SERVER ERROR"
        ))
        .onComplete(ctx.failingThenComplete());
  }
}
