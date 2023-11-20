package org.hyperagents.yggdrasil.http;

import com.google.common.net.HttpHeaders;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxTestContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.junit.jupiter.api.Assertions;

public final class HttpServerVerticleTestHelper {
  public static final int TEST_PORT = 8080;
  public static final String TEST_HOST = "localhost";
  public static final String AGENT_WEBID = "X-Agent-WebID";
  public static final String TEST_AGENT_ID = "test_agent";
  public static final String SLUG_HEADER = "Slug";
  public static final String TURTLE_CONTENT_TYPE = "text/turtle";
  public static final String NAMES_EQUAL_MESSAGE = "The names should be equal";
  public static final String URIS_EQUAL_MESSAGE = "The URIs should be equal";
  public static final String TDS_EQUAL_MESSAGE = "The thing descriptions should be equal";
  public static final String OK_STATUS_MESSAGE = "Status code should be OK";
  public static final String CREATED_STATUS_MESSAGE = "Status code should be CREATED";
  public static final String UNAUTHORIZED_STATUS_MESSAGE = "Status code should be UNAUTHORIZED";
  public static final String RESPONSE_BODY_EMPTY_MESSAGE = "The response body should be empty";

  private final WebClient client;
  private final BlockingQueue<Message<RdfStoreMessage>> storeMessageQueue;

  public HttpServerVerticleTestHelper(
      final WebClient client,
      final BlockingQueue<Message<RdfStoreMessage>> storeMessageQueue
  ) {
    this.client = client;
    this.storeMessageQueue = storeMessageQueue;
  }

  public void testGetResourceSucceeds(
      final VertxTestContext ctx,
      final String resourceRepresentationFilePath,
      final String resourceUri
  ) {
    try {
      final var expectedRepresentation =
          Files.readString(
            Path.of(ClassLoader.getSystemResource(resourceRepresentationFilePath).toURI())
          );
      final var request = this.client.get(TEST_PORT, TEST_HOST, resourceUri).send();
      final var message = this.storeMessageQueue.take();
      final var getEntityMessage = (RdfStoreMessage.GetEntity) message.body();
      Assertions.assertEquals(
          "http://" + TEST_HOST + ":" + TEST_PORT + resourceUri,
          getEntityMessage.requestUri()
      );
      message.reply(expectedRepresentation);
      request
          .onSuccess(r -> {
            Assertions.assertEquals(
                HttpStatus.SC_OK,
                r.statusCode(),
                OK_STATUS_MESSAGE
            );
            Assertions.assertEquals(
                TURTLE_CONTENT_TYPE,
                r.getHeader(HttpHeaders.CONTENT_TYPE),
                "The content type should be text/turtle"
            );
            Assertions.assertEquals(
                expectedRepresentation,
                r.bodyAsString(),
                TDS_EQUAL_MESSAGE
            );
          })
          .onComplete(ctx.succeedingThenComplete());
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  public void testResourceRequestFailsWithNotFound(
      final VertxTestContext ctx,
      final String resourceUri,
      final Future<HttpResponse<Buffer>> request
  ) {
    try {
      final var message = this.storeMessageQueue.take();
      Assertions.assertEquals(
          "http://" + TEST_HOST + ":" + TEST_PORT + resourceUri,
          message.body().requestUri(),
          URIS_EQUAL_MESSAGE
      );
      message.fail(HttpStatus.SC_NOT_FOUND, "The entity was not found");
      request
          .onSuccess(r -> {
            Assertions.assertEquals(
                HttpStatus.SC_NOT_FOUND,
                r.statusCode(),
                "The status code should be NOT FOUND"
            );
            Assertions.assertNull(
                r.body(),
                RESPONSE_BODY_EMPTY_MESSAGE
            );
          })
          .onComplete(ctx.succeedingThenComplete());
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  public void testResourceRequestRedirectsWithAddedSlash(
      final VertxTestContext ctx,
      final HttpMethod method,
      final String resourceUri
  ) {
    try {
      this.client.request(method, TEST_PORT, TEST_HOST, resourceUri + "/")
                 .followRedirects(false)
                 .send()
                 .onSuccess(r -> {
                   Assertions.assertEquals(
                       HttpStatus.SC_MOVED_PERMANENTLY,
                       r.statusCode(),
                       "The status code should be MOVED PERMANENTLY"
                   );
                   Assertions.assertEquals(
                       "http://" + TEST_HOST + ":" + TEST_PORT + resourceUri,
                       r.getHeader(HttpHeaders.LOCATION),
                       "The location should be the same but missing the trailing slash"
                   );
                   Assertions.assertNull(
                       r.body(),
                       RESPONSE_BODY_EMPTY_MESSAGE
                   );
                 })
                 .onComplete(ctx.succeedingThenComplete());
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  public void testPostTurtleResourceSucceeds(
      final VertxTestContext ctx,
      final String resourceRepresentationFilePath,
      final String resourceUri,
      final String resourceName
  ) {
    try {
      final var expectedRepresentation =
          Files.readString(
            Path.of(ClassLoader.getSystemResource(resourceRepresentationFilePath).toURI())
          );
      final var request = this.client.post(TEST_PORT, TEST_HOST, resourceUri)
                                     .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                     .putHeader(SLUG_HEADER, resourceName)
                                     .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
                                     .sendBuffer(Buffer.buffer(expectedRepresentation));
      final var message = this.storeMessageQueue.take();
      final var createResourceMessage = (RdfStoreMessage.CreateEntity) message.body();
      Assertions.assertEquals(
          "http://" + TEST_HOST + ":" + TEST_PORT + resourceUri,
          createResourceMessage.requestUri(),
          URIS_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          resourceName,
          createResourceMessage.entityName(),
          NAMES_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          expectedRepresentation,
          createResourceMessage.entityRepresentation(),
          TDS_EQUAL_MESSAGE
      );
      message.reply(expectedRepresentation);
      request
          .onSuccess(r -> {
            Assertions.assertEquals(
                HttpStatus.SC_CREATED,
                r.statusCode(),
                CREATED_STATUS_MESSAGE
            );
            Assertions.assertEquals(
                TURTLE_CONTENT_TYPE,
                r.getHeader(HttpHeaders.CONTENT_TYPE),
                "The content type should be text/turtle"
            );
            Assertions.assertEquals(
                expectedRepresentation,
                r.bodyAsString(),
                TDS_EQUAL_MESSAGE
            );
          })
          .onComplete(ctx.succeedingThenComplete());
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  public void testResourceRequestFailsWithoutWebId(
      final VertxTestContext ctx,
      final Future<HttpResponse<Buffer>> request
  ) {
    request
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_UNAUTHORIZED,
              r.statusCode(),
              UNAUTHORIZED_STATUS_MESSAGE
          );
          Assertions.assertEquals(
              "Unauthorized",
              r.bodyAsString(),
              "The response bodies should be equal"
          );
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  public void testPutTurtleResourceSucceeds(
      final VertxTestContext ctx,
      final String resourceUri,
      final String resourceRepresentationFilePath
  ) {
    try {
      final var expectedRepresentation =
          Files.readString(Path.of(
            ClassLoader.getSystemResource(resourceRepresentationFilePath).toURI()
          ));
      final var request = this.client.put(TEST_PORT, TEST_HOST, resourceUri)
                                     .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                     .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
                                     .sendBuffer(Buffer.buffer(expectedRepresentation));
      final var message = this.storeMessageQueue.take();
      final var updateResourceMessage = (RdfStoreMessage.UpdateEntity) message.body();
      Assertions.assertEquals(
          "http://" + TEST_HOST + ":" + TEST_PORT + resourceUri,
          updateResourceMessage.requestUri(),
          URIS_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          expectedRepresentation,
          updateResourceMessage.entityRepresentation(),
          TDS_EQUAL_MESSAGE
      );
      message.reply(expectedRepresentation);
      request
          .onSuccess(r -> {
            Assertions.assertEquals(
                HttpStatus.SC_OK,
                r.statusCode(),
                OK_STATUS_MESSAGE
            );
            Assertions.assertEquals(
                TURTLE_CONTENT_TYPE,
                r.getHeader(HttpHeaders.CONTENT_TYPE),
                "The content type should be text/turtle"
            );
            Assertions.assertEquals(
                expectedRepresentation,
                r.bodyAsString(),
                TDS_EQUAL_MESSAGE
            );
          })
          .onComplete(ctx.succeedingThenComplete());
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  public void testResourceRequestFailsWithoutContentType(
      final VertxTestContext ctx,
      final HttpMethod method,
      final String resourceUri,
      final Buffer content
  ) {
    try {
      this.client.request(method, TEST_PORT, TEST_HOST, resourceUri)
                 .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                 .sendBuffer(content)
                 .onSuccess(r -> {
                   Assertions.assertEquals(
                       HttpStatus.SC_BAD_REQUEST,
                       r.statusCode(),
                       UNAUTHORIZED_STATUS_MESSAGE
                   );
                   Assertions.assertNull(
                       r.bodyAsString(),
                       RESPONSE_BODY_EMPTY_MESSAGE
                   );
                 })
                 .onComplete(ctx.succeedingThenComplete());
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  public void testDeleteTurtleResourceSucceeds(
      final VertxTestContext ctx,
      final String resourceUri,
      final String entityRepresentationFileName
  ) {
    try {
      final var expectedRepresentation =
          Files.readString(Path.of(
            ClassLoader.getSystemResource(entityRepresentationFileName).toURI()
          ));
      final var request = this.client.delete(TEST_PORT, TEST_HOST, resourceUri)
                                     .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                     .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
                                     .send();
      final var message = this.storeMessageQueue.take();
      final var updateResourceMessage = (RdfStoreMessage.DeleteEntity) message.body();
      Assertions.assertEquals(
          "http://" + TEST_HOST + ":" + TEST_PORT + resourceUri,
          updateResourceMessage.requestUri(),
          URIS_EQUAL_MESSAGE
      );
      message.reply(expectedRepresentation);
      request
          .onSuccess(r -> {
            Assertions.assertEquals(
                HttpStatus.SC_OK,
                r.statusCode(),
                OK_STATUS_MESSAGE
            );
            Assertions.assertEquals(
                expectedRepresentation,
                r.bodyAsString(),
                TDS_EQUAL_MESSAGE
            );
          })
          .onComplete(ctx.succeedingThenComplete());
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }
}
