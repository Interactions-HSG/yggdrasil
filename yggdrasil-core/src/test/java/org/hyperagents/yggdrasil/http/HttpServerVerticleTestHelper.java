package org.hyperagents.yggdrasil.http;

import com.google.common.net.HttpHeaders;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.junit.jupiter.api.Assertions;

/**
 * test helper class.
 */
@SuppressFBWarnings("EI_EXPOSE_REP2")
@SuppressWarnings("PMD.JUnit4TestShouldUseTestAnnotation")
public final class HttpServerVerticleTestHelper {
  private static final int TEST_PORT = 8080;
  private static final String TEST_HOST = "localhost";
  private static final String AGENT_WEBID = "X-Agent-WebID";
  private static final String TEST_AGENT_ID = "test_agent";
  private static final String TURTLE_CONTENT_TYPE = "text/turtle";
  private static final String URIS_EQUAL_MESSAGE = "The URIs should be equal";
  private static final String TDS_EQUAL_MESSAGE = "The thing descriptions should be equal";
  private static final String OK_STATUS_MESSAGE = "Status code should be OK";
  private static final String UNAUTHORIZED_STATUS_MESSAGE = "Status code should be UNAUTHORIZED";
  private static final String RESPONSE_BODY_EMPTY_MESSAGE = "The response body should be empty";
  private static final String RESPONSE_BODY_BAD_REQUEST_MESSAGE =
      "Status code should be Bad Request";

  private final WebClient client;
  private final BlockingQueue<Message<RdfStoreMessage>> storeMessageQueue;

  public HttpServerVerticleTestHelper(
      final WebClient client,
      final BlockingQueue<Message<RdfStoreMessage>> storeMessageQueue
  ) {
    this.client = client;
    this.storeMessageQueue = storeMessageQueue;
  }

  void testGetResourceSucceeds(
      final VertxTestContext ctx,
      final String resourceRepresentationFilePath,
      final String resourceUri
  ) throws URISyntaxException, IOException, InterruptedException {
    final var expectedRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource(resourceRepresentationFilePath).toURI()),
            StandardCharsets.UTF_8
        );
    final var request = this.client.get(TEST_PORT, TEST_HOST, resourceUri).send();
    final var message = this.storeMessageQueue.take();
    final var getEntityMessage = (RdfStoreMessage.GetEntity) message.body();
    Assertions.assertEquals(
        this.getUri(resourceUri),
        getEntityMessage.requestUri(),
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
  }

  void testResourceRequestFailsWithNotFound(
      final VertxTestContext ctx,
      final String resourceUri,
      final String workspaceName,
      final String artifactName,
      final Future<HttpResponse<Buffer>> request
  ) throws InterruptedException {
    final var message = this.storeMessageQueue.take();
    if (message.body() instanceof RdfStoreMessage.GetEntity(String requestUri)) {
      Assertions.assertEquals(
          this.getUri(resourceUri),
          requestUri,
          URIS_EQUAL_MESSAGE
      );
    } else if (message.body() instanceof RdfStoreMessage.ReplaceEntity m) {
      Assertions.assertEquals(
          this.getUri(resourceUri),
          m.requestUri(),
          URIS_EQUAL_MESSAGE
      );
    } else if (message.body() instanceof RdfStoreMessage.DeleteEntity m) {
      Assertions.assertEquals(
          artifactName,
          m.artifactName(),
          URIS_EQUAL_MESSAGE
      );
    } else {
      Assertions.fail("Not an intended use of the helper method");
    }
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
  }

  void testResourceRequestRedirectsWithAddedSlash(
      final VertxTestContext ctx,
      final HttpMethod method,
      final String resourceUri
  ) {
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
              this.getUri(resourceUri),
              r.getHeader(HttpHeaders.LOCATION),
              "The location should be the same but without the trailing slash"
          );
          Assertions.assertNull(
              r.body(),
              RESPONSE_BODY_EMPTY_MESSAGE
          );
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  void testResourceRequestFailsWithoutWebId(
      final VertxTestContext ctx,
      final Future<HttpResponse<Buffer>> request
  ) {
    request
        .onSuccess(r -> Assertions.assertEquals(
            HttpStatus.SC_UNAUTHORIZED,
            r.statusCode(),
            UNAUTHORIZED_STATUS_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  void testPutTurtleResourceSucceeds(
      final VertxTestContext ctx,
      final String resourceUri,
      final String resourceRepresentationFilePath
  ) throws InterruptedException, URISyntaxException, IOException {
    final var expectedRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource(resourceRepresentationFilePath).toURI()),
            StandardCharsets.UTF_8
        );
    final var request = this.client.put(TEST_PORT, TEST_HOST, resourceUri)
        .putHeader(AGENT_WEBID, TEST_AGENT_ID)
        .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
        .sendBuffer(Buffer.buffer(expectedRepresentation));
    final var message = this.storeMessageQueue.take();
    final var updateResourceMessage = (RdfStoreMessage.ReplaceEntity) message.body();
    Assertions.assertEquals(
        this.getUri(resourceUri),
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
  }

  void testResourceRequestFailsWithoutContentType(
      final VertxTestContext ctx,
      final HttpMethod method,
      final String resourceUri,
      final Buffer content
  ) {
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
              RESPONSE_BODY_BAD_REQUEST_MESSAGE
          );
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  void testDeleteTurtleResourceSucceeds(
      final VertxTestContext ctx,
      final String resourceUri,
      final String entityRepresentationFileName
  ) throws InterruptedException, URISyntaxException, IOException {
    final var expectedRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource(entityRepresentationFileName).toURI()),
            StandardCharsets.UTF_8
        );
    final var request = this.client.delete(TEST_PORT, TEST_HOST, resourceUri)
        .putHeader(AGENT_WEBID, TEST_AGENT_ID)
        .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
        .send();
    final var message = this.storeMessageQueue.take();
    final var deleteResourceMessage = (RdfStoreMessage.DeleteEntity) message.body();
    Assertions.assertEquals(
        "test",
        deleteResourceMessage.workspaceName(),
        URIS_EQUAL_MESSAGE
    );
    Assertions.assertNull(deleteResourceMessage.artifactName(), URIS_EQUAL_MESSAGE);
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
  }

  void testDeleteTurtleArtifactSucceeds(
      final VertxTestContext ctx,
      final String resourceUri,
      final String entityRepresentationFileName
  ) throws InterruptedException, URISyntaxException, IOException {
    final var expectedRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource(entityRepresentationFileName).toURI()),
            StandardCharsets.UTF_8
        );
    final var request = this.client.delete(TEST_PORT, TEST_HOST, resourceUri)
        .putHeader(AGENT_WEBID, TEST_AGENT_ID)
        .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
        .send();
    final var message = this.storeMessageQueue.take();
    final var deleteResourceMessage = (RdfStoreMessage.DeleteEntity) message.body();
    Assertions.assertEquals(
        "test",
        deleteResourceMessage.workspaceName(),
        URIS_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        "c0",
        deleteResourceMessage.artifactName(),
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
  }


  public String getUri(final String path) {
    return "http://" + TEST_HOST + ":" + TEST_PORT + path;
  }
}
