package org.hyperagents.yggdrasil.http;

import com.google.common.net.HttpHeaders;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@ExtendWith(VertxExtension.class)
public class QueryHttpHandlersTest {
  private static final int TEST_PORT = 8080;
  private static final String TEST_HOST = "localhost";
  private static final String PLATFORM_URI = "http://" + TEST_HOST + ":" + TEST_PORT + "/";
  public static final String THING_PARAM = "thing";
  public static final String NAME_PARAM = "name";
  public static final String NAME_PARAM_VALUE = "createWorkspace";

  private final BlockingQueue<Message<RdfStoreMessage>> messageQueue;

  public QueryHttpHandlersTest() {
    this.messageQueue = new LinkedBlockingQueue<>();
  }

  private WebClient client;

  @BeforeEach
  public void setUp(final Vertx vertx, final VertxTestContext ctx) {
    this.client = WebClient.create(vertx);
    vertx.deployVerticle(new HttpServerVerticle(), ctx.succeedingThenComplete());
    final var storeMessagebox = new RdfStoreMessagebox(vertx.eventBus());
    storeMessagebox.init();
    storeMessagebox.receiveMessages(this.messageQueue::add);
  }

  @AfterEach
  public void tearDown(final Vertx vertx, final VertxTestContext tc) {
    vertx.close(tc.succeedingThenComplete());
  }

  @Test
  public void testQueryRequest(final VertxTestContext ctx) throws InterruptedException {
    final var query =
        """
           PREFIX wot: <https://www.w3.org/2019/wot/td#>
           PREFIX eve: <http://w3id.org/eve#>
           SELECT ?thing ?name
           WHERE {
             ?thing wot:hasActionAffordance [
               a eve:MakeWorkspace;
               wot:name ?name
             ].
           }
           """;
    final var request = this.client.get(TEST_PORT, TEST_HOST, "/query")
                                   .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
                                   .sendBuffer(Buffer.buffer(query));
    final var message = this.messageQueue.take();
    Assertions.assertEquals(
      query,
      ((RdfStoreMessage.Query) message.body()).query(),
      "The queries should be equal"
    );
    final var result =
        JsonArray.of(JsonObject.of(THING_PARAM, PLATFORM_URI, NAME_PARAM, NAME_PARAM_VALUE))
                 .encode();
    message.reply(result);
    request
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
            "Status code should be OK"
          );
          Assertions.assertEquals(
              result,
              r.bodyAsString(),
              "The query result should be the expected one"
          );
        })
        .onComplete(ctx.succeedingThenComplete());
  }
}
