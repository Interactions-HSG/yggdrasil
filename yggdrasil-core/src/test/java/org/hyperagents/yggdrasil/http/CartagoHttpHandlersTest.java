package org.hyperagents.yggdrasil.http;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.eventbus.messageboxes.CartagoMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class CartagoHttpHandlersTest {
  private static final int TEST_PORT = 8080;
  private static final String TEST_HOST = "localhost";
  private static final String AGENT_WEBID = "X-Agent-WebID";
  private static final String TEST_AGENT_ID = "test_agent";
  private static final String SLUG_HEADER = "Slug";
  private static final String MAIN_WORKSPACE_NAME = "test";
  private static final String SUB_WORKSPACE_NAME = "sub";
  private static final String WORKSPACES_PATH = "/workspaces/";
  private static final String MAIN_WORKSPACE_PATH = WORKSPACES_PATH + MAIN_WORKSPACE_NAME;
  private static final String NAMES_EQUAL_MESSAGE = "The names should be equal";
  private static final String URIS_EQUAL_MESSAGE = "The URIs should be equal";
  private static final String TDS_EQUAL_MESSAGE = "The thing descriptions should be equal";
  private static final String CREATED_STATUS_MESSAGE = "Status code should be CREATED";
  private static final String RESPONSE_BODY_EMPTY_MESSAGE = "The response body should be empty";

  private final BlockingQueue<Message<RdfStoreMessage>> storeMessageQueue;
  private final BlockingQueue<Message<CartagoMessage>> cartagoMessageQueue;
  private WebClient client;
  private HttpServerVerticleTestHelper helper;

  public CartagoHttpHandlersTest() {
    this.storeMessageQueue = new LinkedBlockingQueue<>();
    this.cartagoMessageQueue = new LinkedBlockingQueue<>();
  }

  @BeforeEach
  public void setUp(final Vertx vertx, final VertxTestContext ctx) {
    this.client = WebClient.create(vertx);
    this.helper = new HttpServerVerticleTestHelper(this.client, this.storeMessageQueue);
    final var storeMessagebox = new RdfStoreMessagebox(vertx.eventBus());
    storeMessagebox.init();
    storeMessagebox.receiveMessages(this.storeMessageQueue::add);
    final var cartagoMessagebox = new CartagoMessagebox(vertx.eventBus());
    cartagoMessagebox.init();
    cartagoMessagebox.receiveMessages(this.cartagoMessageQueue::add);
    vertx.deployVerticle(new HttpServerVerticle(), ctx.succeedingThenComplete());
  }

  @AfterEach
  public void tearDown(final Vertx vertx, final VertxTestContext ctx) {
    vertx.close(ctx.succeedingThenComplete());
  }

  @Test
  public void testPostWorkspacesSucceeds(final VertxTestContext ctx) {
    try {
      final var expectedWorkspaceRepresentation =
          Files.readString(
            Path.of(ClassLoader.getSystemResource("test_workspace_td.ttl").toURI())
          );
      final var request = this.client.post(TEST_PORT, TEST_HOST, WORKSPACES_PATH)
                                     .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                     .putHeader(SLUG_HEADER, MAIN_WORKSPACE_NAME)
                                     .send();
      final var cartagoMessage = this.cartagoMessageQueue.take();
      final var createWorkspaceMessage = (CartagoMessage.CreateWorkspace) cartagoMessage.body();
      Assertions.assertEquals(
          MAIN_WORKSPACE_NAME,
          createWorkspaceMessage.workspaceName(),
          NAMES_EQUAL_MESSAGE
      );
      cartagoMessage.reply(expectedWorkspaceRepresentation);
      final var storeMessage = this.storeMessageQueue.take();
      final var createEntityMessage = (RdfStoreMessage.CreateEntity) storeMessage.body();
      Assertions.assertEquals(
          MAIN_WORKSPACE_NAME,
          createEntityMessage.entityName(),
          NAMES_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          "http://" + TEST_HOST + ":" + TEST_PORT + WORKSPACES_PATH,
          createEntityMessage.requestUri(),
          URIS_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          expectedWorkspaceRepresentation,
          createEntityMessage.entityRepresentation(),
          TDS_EQUAL_MESSAGE
      );
      storeMessage.reply(expectedWorkspaceRepresentation);
      request
          .onSuccess(r -> {
            Assertions.assertEquals(
                HttpStatus.SC_CREATED,
                r.statusCode(),
                CREATED_STATUS_MESSAGE
            );
            Assertions.assertEquals(
                expectedWorkspaceRepresentation,
                r.bodyAsString(),
                TDS_EQUAL_MESSAGE
            );
          })
          .onComplete(ctx.succeedingThenComplete());
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostWorkspacesFailsWithoutWebId(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestFailsWithoutWebId(
          ctx,
          this.client.post(TEST_PORT, TEST_HOST, WORKSPACES_PATH)
                     .putHeader(SLUG_HEADER, MAIN_WORKSPACE_NAME)
                     .send()
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostSubWorkspace(final VertxTestContext ctx) {
    try {
      final var expectedWorkspaceRepresentation =
          Files.readString(
            Path.of(ClassLoader.getSystemResource("sub_workspace_td.ttl").toURI())
          );
      final var request = this.client.post(TEST_PORT, TEST_HOST, MAIN_WORKSPACE_PATH)
                                     .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                     .putHeader(SLUG_HEADER, SUB_WORKSPACE_NAME)
                                     .send();
      final var cartagoMessage = this.cartagoMessageQueue.take();
      final var createSubWorkspaceMessage =
          (CartagoMessage.CreateSubWorkspace) cartagoMessage.body();
      Assertions.assertEquals(
          SUB_WORKSPACE_NAME,
          createSubWorkspaceMessage.subWorkspaceName(),
          NAMES_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          MAIN_WORKSPACE_NAME,
          createSubWorkspaceMessage.workspaceName(),
          NAMES_EQUAL_MESSAGE
      );
      cartagoMessage.reply(expectedWorkspaceRepresentation);
      final var storeMessage = this.storeMessageQueue.take();
      final var createEntityMessage = (RdfStoreMessage.CreateEntity) storeMessage.body();
      Assertions.assertEquals(
          SUB_WORKSPACE_NAME,
          createEntityMessage.entityName(),
          NAMES_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          "http://" + TEST_HOST + ":" + TEST_PORT + WORKSPACES_PATH,
          createEntityMessage.requestUri(),
          URIS_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          expectedWorkspaceRepresentation,
          createEntityMessage.entityRepresentation(),
          TDS_EQUAL_MESSAGE
      );
      storeMessage.reply(expectedWorkspaceRepresentation);
      request
          .onSuccess(r -> {
            Assertions.assertEquals(
                HttpStatus.SC_CREATED,
                r.statusCode(),
                CREATED_STATUS_MESSAGE
            );
            Assertions.assertEquals(
                expectedWorkspaceRepresentation,
                r.bodyAsString(),
                TDS_EQUAL_MESSAGE
            );
          })
          .onComplete(ctx.succeedingThenComplete());
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostSubWorkspaceWithParentNotFound(final VertxTestContext ctx) {
    try {
      final var request = this.client.post(TEST_PORT, TEST_HOST, WORKSPACES_PATH + "nonexistent")
                                     .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                     .putHeader(SLUG_HEADER, SUB_WORKSPACE_NAME)
                                     .send();
      final var message = this.cartagoMessageQueue.take();
      final var createSubWorkspaceMessage =
          (CartagoMessage.CreateSubWorkspace) message.body();
      Assertions.assertEquals(
          "nonexistent",
          createSubWorkspaceMessage.workspaceName(),
          NAMES_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          SUB_WORKSPACE_NAME,
          createSubWorkspaceMessage.subWorkspaceName(),
          NAMES_EQUAL_MESSAGE
      );
      message.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR, "The entity was not found");
      request
          .onSuccess(r -> {
            Assertions.assertEquals(
                HttpStatus.SC_INTERNAL_SERVER_ERROR,
                r.statusCode(),
                "The status code should be INTERNAL SERVER ERROR"
            );
            Assertions.assertEquals(
                "Internal Server Error",
                r.bodyAsString(),
                RESPONSE_BODY_EMPTY_MESSAGE
            );
          })
          .onComplete(ctx.succeedingThenComplete());
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostSubWorkspaceFailsWithoutWebId(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestFailsWithoutWebId(
          ctx,
          this.client.post(TEST_PORT, TEST_HOST, MAIN_WORKSPACE_PATH)
                     .putHeader(SLUG_HEADER, SUB_WORKSPACE_NAME)
                     .send()
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostSubWorkspaceRedirectsWithSlash(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestRedirectsWithAddedSlash(
          ctx,
          HttpMethod.POST,
          MAIN_WORKSPACE_PATH
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  /*
  @Test
  @Disabled
  public void testCartagoArtifact(final VertxTestContext tc) {
    // Register artifact template for this test
    final var knownArtifacts =
        new JsonObject()
          .put("http://example.org/Counter", "org.hyperagents.yggdrasil.cartago.artifacts.Counter");

    this.vertx.deployVerticle(
        CartagoVerticle.class.getName(),
        new DeploymentOptions().setWorker(true)
                               .setConfig(new JsonObject().put("known-artifacts", knownArtifacts)),
        tc.succeedingThenComplete()
    );

    final var checkpoint = tc.checkpoint();

    // TODO: This test seems wrong. Why would there be a localhost:8080/workspaces path?
    this.client
        .post(TEST_PORT, TEST_HOST, "/workspaces/")
        .putHeader(AGENT_WEBID, "http://andreiciortea.ro/#me")
        .putHeader(SLUG_HEADER, "wksp1")
        .sendBuffer(Buffer.buffer(""), wkspAR -> {
          Assertions.assertEquals(
              HttpStatus.SC_CREATED,
              wkspAR.result().statusCode(),
              CREATED_STATUS_MESSAGE
          );

          this.client
              .post(TEST_PORT, TEST_HOST, TEST_WORKSPACE_PATH + "/artifacts/")
              .putHeader(AGENT_WEBID, "http://andreiciortea.ro/#me")
              .putHeader(SLUG_HEADER, "c0")
              .putHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType())
              .sendBuffer(
                Buffer.buffer("{\"artifactClass\" : \"http://example.org/Counter\"}"),
                ar -> {
                  System.out.println("artifact created");
                  Assertions.assertEquals(
                      HttpStatus.SC_CREATED,
                      ar.result().statusCode(),
                      CREATED_STATUS_MESSAGE
                  );

                  this.client
                      .post(
                          TEST_PORT,
                          TEST_HOST,
                          TEST_WORKSPACE_PATH + "/artifacts/c0/increment"
                      )
                      .putHeader(AGENT_WEBID, "http://andreiciortea.ro/#me")
                      .putHeader(CONTENT_TYPE_HEADER, ContentType.APPLICATION_JSON.getMimeType())
                      .sendBuffer(
                        Buffer.buffer("[1]"),
                        actionAr -> {
                          System.out.println("operation executed");
                          Assertions.assertEquals(
                              HttpStatus.SC_OK,
                              actionAr.result().statusCode(),
                              OK_STATUS_MESSAGE
                          );
                          checkpoint.flag();
                        }
                      );
                }
              );
        });
  }
 */
}
