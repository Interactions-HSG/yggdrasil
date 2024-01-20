package org.hyperagents.yggdrasil.http;

import com.google.common.net.HttpHeaders;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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
public class QueryHttpHandlersTest {
  private static final int TEST_PORT = 8080;
  private static final String TEST_HOST = "localhost";
  private static final String PLATFORM_URI = "http://" + TEST_HOST + ":" + TEST_PORT + "/";
  private static final String QUERY =
      """
        PREFIX td: <https://www.w3.org/2019/wot/td#>
        PREFIX hmas: <https://purl.org/hmas/>
        PREFIX ex: <http://example.org/>

        ASK WHERE {
            ?workspace hmas:contains [
                           a hmas:Artifact, ex:Counter;
                           td:title ?artifact;
                       ];
                       a hmas:Workspace.
        }
        """;
  private static final String QUERY_ENDPOINT = "/query";
  private static final String CSV_MIME_TYPE = "text/csv";
  private static final String XML_MIME_TYPE = "application/sparql-results+xml";
  private static final String QUERY_PARAM = "query";
  private static final String DEFAULT_GRAPH_URI_PARAM = "default-graph-uri";
  private static final String NAMED_GRAPH_URI_PARAM = "named-graph-uri";
  private static final String CONTENT_TYPES_EQUAL_MESSAGE = "The content types should be equal";
  private static final String BAD_REQUEST_STATUS_CODE_MESSAGE = "Status code should be BAD REQUEST";
  private static final String BAD_REQUEST_CONTENT = "Bad Request";
  private static final String RESULT_STATUS_CODE_MESSAGE =
      "The query result should contain the status code";

  private final BlockingQueue<Message<RdfStoreMessage>> messageQueue;
  private WebClient client;

  public QueryHttpHandlersTest() {
    this.messageQueue = new LinkedBlockingQueue<>();
  }

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
  public void testGetQueryRequest(final VertxTestContext ctx) throws InterruptedException {
    final var request =
        this.client.get(TEST_PORT, TEST_HOST, QUERY_ENDPOINT)
                   .putHeader(HttpHeaders.ACCEPT, XML_MIME_TYPE)
                   .addQueryParam(QUERY_PARAM, URLEncoder.encode(QUERY, StandardCharsets.UTF_8))
                   .addQueryParam(
                     DEFAULT_GRAPH_URI_PARAM,
                     URLEncoder.encode(PLATFORM_URI, StandardCharsets.UTF_8)
                   )
                   .addQueryParam(
                     NAMED_GRAPH_URI_PARAM,
                     URLEncoder.encode(PLATFORM_URI, StandardCharsets.UTF_8)
                   )
                   .send();
    final var message = this.messageQueue.take();
    final var queryMessage = (RdfStoreMessage.QueryKnowledgeGraph) message.body();
    Assertions.assertEquals(
        QUERY,
        queryMessage.query(),
        "The queries should be equal"
    );
    Assertions.assertEquals(
        List.of(PLATFORM_URI),
        queryMessage.defaultGraphUris(),
        "The default graph URIs should be equal"
    );
    Assertions.assertEquals(
        List.of(PLATFORM_URI),
        queryMessage.namedGraphUris(),
        "The named graph URIs should be equal"
    );
    Assertions.assertEquals(
        XML_MIME_TYPE,
        queryMessage.responseContentType(),
        CONTENT_TYPES_EQUAL_MESSAGE
    );
    final var result =
        """
          <?xml version='1.0' encoding='UTF-8'?>
          <sparql xmlns='http://www.w3.org/2005/sparql-results#'>
            <head>
            </head>
            <boolean>true</boolean>
          </sparql>
          """;
    message.reply(result);
    request
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              "Status code should be OK"
          );
          Assertions.assertEquals(
              XML_MIME_TYPE,
              r.getHeader(HttpHeaders.CONTENT_TYPE),
              CONTENT_TYPES_EQUAL_MESSAGE
          );
          Assertions.assertEquals(
              result,
              r.bodyAsString(),
              "The query result should be the expected one"
          );
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testGetQueryRequestWithBody(final VertxTestContext ctx) {
    this.client.get(TEST_PORT, TEST_HOST, QUERY_ENDPOINT)
               .putHeader(HttpHeaders.ACCEPT, XML_MIME_TYPE)
               .addQueryParam(QUERY_PARAM, URLEncoder.encode(QUERY, StandardCharsets.UTF_8))
               .addQueryParam(
                 DEFAULT_GRAPH_URI_PARAM,
                 URLEncoder.encode(PLATFORM_URI, StandardCharsets.UTF_8)
               )
               .addQueryParam(
                 NAMED_GRAPH_URI_PARAM,
                 URLEncoder.encode(PLATFORM_URI, StandardCharsets.UTF_8)
               )
               .sendBuffer(Buffer.buffer(QUERY))
               .onSuccess(r -> {
                 Assertions.assertEquals(
                     HttpStatus.SC_BAD_REQUEST,
                     r.statusCode(),
                     BAD_REQUEST_STATUS_CODE_MESSAGE
                 );
                 Assertions.assertEquals(
                     BAD_REQUEST_CONTENT,
                     r.bodyAsString(),
                     RESULT_STATUS_CODE_MESSAGE
                 );
               })
               .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testGetQueryRequestWithMultipleQueries(final VertxTestContext ctx) {
    this.client.get(TEST_PORT, TEST_HOST, QUERY_ENDPOINT)
               .putHeader(HttpHeaders.ACCEPT, XML_MIME_TYPE)
               .addQueryParam(QUERY_PARAM, URLEncoder.encode(QUERY, StandardCharsets.UTF_8))
               .addQueryParam(QUERY_PARAM, URLEncoder.encode(QUERY, StandardCharsets.UTF_8))
               .addQueryParam(
                 DEFAULT_GRAPH_URI_PARAM,
                 URLEncoder.encode(PLATFORM_URI, StandardCharsets.UTF_8)
               )
               .addQueryParam(
                 NAMED_GRAPH_URI_PARAM,
                 URLEncoder.encode(PLATFORM_URI, StandardCharsets.UTF_8)
               )
               .send()
               .onSuccess(r -> {
                 Assertions.assertEquals(
                     HttpStatus.SC_BAD_REQUEST,
                     r.statusCode(),
                     BAD_REQUEST_STATUS_CODE_MESSAGE
                 );
                 Assertions.assertEquals(
                     BAD_REQUEST_CONTENT,
                     r.bodyAsString(),
                     RESULT_STATUS_CODE_MESSAGE
                 );
               })
               .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testGetQueryRequestWithoutQuery(final VertxTestContext ctx) {
    this.client.get(TEST_PORT, TEST_HOST, QUERY_ENDPOINT)
               .putHeader(HttpHeaders.ACCEPT, XML_MIME_TYPE)
               .addQueryParam(
                 DEFAULT_GRAPH_URI_PARAM,
                 URLEncoder.encode(PLATFORM_URI, StandardCharsets.UTF_8)
               )
               .addQueryParam(
                 NAMED_GRAPH_URI_PARAM,
                 URLEncoder.encode(PLATFORM_URI, StandardCharsets.UTF_8)
               )
               .send()
               .onSuccess(r -> {
                 Assertions.assertEquals(
                     HttpStatus.SC_BAD_REQUEST,
                     r.statusCode(),
                     BAD_REQUEST_STATUS_CODE_MESSAGE
                 );
                 Assertions.assertEquals(
                     BAD_REQUEST_CONTENT,
                     r.bodyAsString(),
                     RESULT_STATUS_CODE_MESSAGE
                 );
               })
               .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testPostFormQueryRequest(final VertxTestContext ctx) throws InterruptedException {
    final var request =
        this.client.post(TEST_PORT, TEST_HOST, QUERY_ENDPOINT)
                   .putHeader(HttpHeaders.ACCEPT, CSV_MIME_TYPE)
                   .sendForm(
                     MultiMap.caseInsensitiveMultiMap()
                             .add(QUERY_PARAM, URLEncoder.encode(QUERY, StandardCharsets.UTF_8))
                             .add(
                               DEFAULT_GRAPH_URI_PARAM,
                               URLEncoder.encode(PLATFORM_URI, StandardCharsets.UTF_8)
                             )
                             .add(
                               NAMED_GRAPH_URI_PARAM,
                               URLEncoder.encode(PLATFORM_URI, StandardCharsets.UTF_8)
                             )
                   );
    final var message = this.messageQueue.take();
    final var queryMessage = (RdfStoreMessage.QueryKnowledgeGraph) message.body();
    Assertions.assertEquals(
        QUERY,
        queryMessage.query(),
        "The queries should be equal"
    );
    Assertions.assertEquals(
        List.of(PLATFORM_URI),
        queryMessage.defaultGraphUris(),
        "The default graph URIs should be equal"
    );
    Assertions.assertEquals(
        List.of(PLATFORM_URI),
        queryMessage.namedGraphUris(),
        "The named graph URIs should be equal"
    );
    Assertions.assertEquals(
        CSV_MIME_TYPE,
        queryMessage.responseContentType(),
        CONTENT_TYPES_EQUAL_MESSAGE
    );
    final var result = "true";
    message.reply(result);
    request
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              "Status code should be OK"
          );
          Assertions.assertEquals(
              CSV_MIME_TYPE,
              r.getHeader(HttpHeaders.CONTENT_TYPE),
              CONTENT_TYPES_EQUAL_MESSAGE
          );
          Assertions.assertEquals(
              result,
              r.bodyAsString(),
              "The query result should be the expected one"
          );
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testPostFormQueryRequestWithMultipleQueries(final VertxTestContext ctx) {
    this.client.post(TEST_PORT, TEST_HOST, QUERY_ENDPOINT)
               .putHeader(HttpHeaders.ACCEPT, CSV_MIME_TYPE)
               .sendForm(
                 MultiMap.caseInsensitiveMultiMap()
                         .add(QUERY_PARAM, URLEncoder.encode(QUERY, StandardCharsets.UTF_8))
                         .add(QUERY_PARAM, URLEncoder.encode(QUERY, StandardCharsets.UTF_8))
                         .add(
                           DEFAULT_GRAPH_URI_PARAM,
                           URLEncoder.encode(PLATFORM_URI, StandardCharsets.UTF_8)
                         )
                         .add(
                           NAMED_GRAPH_URI_PARAM,
                           URLEncoder.encode(PLATFORM_URI, StandardCharsets.UTF_8)
                         )
               )
               .onSuccess(r -> {
                 Assertions.assertEquals(
                     HttpStatus.SC_BAD_REQUEST,
                     r.statusCode(),
                     BAD_REQUEST_STATUS_CODE_MESSAGE
                 );
                 Assertions.assertEquals(
                     BAD_REQUEST_CONTENT,
                     r.bodyAsString(),
                     RESULT_STATUS_CODE_MESSAGE
                 );
               })
              .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testPostFormQueryRequestWithoutQuery(final VertxTestContext ctx) {
    this.client.post(TEST_PORT, TEST_HOST, QUERY_ENDPOINT)
               .putHeader(HttpHeaders.ACCEPT, CSV_MIME_TYPE)
               .sendForm(
                 MultiMap.caseInsensitiveMultiMap()
                         .add(
                           DEFAULT_GRAPH_URI_PARAM,
                           URLEncoder.encode(PLATFORM_URI, StandardCharsets.UTF_8)
                         )
                         .add(
                           NAMED_GRAPH_URI_PARAM,
                           URLEncoder.encode(PLATFORM_URI, StandardCharsets.UTF_8)
                         )
               )
               .onSuccess(r -> {
                 Assertions.assertEquals(
                     HttpStatus.SC_BAD_REQUEST,
                     r.statusCode(),
                     BAD_REQUEST_STATUS_CODE_MESSAGE
                 );
                 Assertions.assertEquals(
                     BAD_REQUEST_CONTENT,
                     r.bodyAsString(),
                     RESULT_STATUS_CODE_MESSAGE
                 );
               })
              .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testSimplePostQueryRequest(final VertxTestContext ctx) throws InterruptedException {
    final var request =
        this.client.post(TEST_PORT, TEST_HOST, QUERY_ENDPOINT)
                   .putHeader(HttpHeaders.ACCEPT, "text/tab-separated-values")
                   .putHeader(HttpHeaders.CONTENT_TYPE, "application/sparql-query")
                   .addQueryParam(
                     DEFAULT_GRAPH_URI_PARAM,
                     URLEncoder.encode(PLATFORM_URI, StandardCharsets.UTF_8)
                   )
                   .addQueryParam(
                     NAMED_GRAPH_URI_PARAM,
                     URLEncoder.encode(PLATFORM_URI, StandardCharsets.UTF_8)
                   )
                   .sendBuffer(Buffer.buffer(QUERY));
    final var message = this.messageQueue.take();
    final var queryMessage = (RdfStoreMessage.QueryKnowledgeGraph) message.body();
    Assertions.assertEquals(
        QUERY,
        queryMessage.query(),
        "The queries should be equal"
    );
    Assertions.assertEquals(
        List.of(PLATFORM_URI),
        queryMessage.defaultGraphUris(),
        "The default graph URIs should be equal"
    );
    Assertions.assertEquals(
        List.of(PLATFORM_URI),
        queryMessage.namedGraphUris(),
        "The named graph URIs should be equal"
    );
    Assertions.assertEquals(
        "text/tab-separated-values",
        queryMessage.responseContentType(),
        CONTENT_TYPES_EQUAL_MESSAGE
    );
    final var result = "true";
    message.reply(result);
    request
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              "Status code should be OK"
          );
          Assertions.assertEquals(
              "text/tab-separated-values",
              r.getHeader(HttpHeaders.CONTENT_TYPE),
              CONTENT_TYPES_EQUAL_MESSAGE
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
