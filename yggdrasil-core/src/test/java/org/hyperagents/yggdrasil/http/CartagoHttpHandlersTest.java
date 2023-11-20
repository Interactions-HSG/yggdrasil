package org.hyperagents.yggdrasil.http;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
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
  private static final String ARTIFACTS_PATH = "/artifacts/";
  private static final String MAIN_ARTIFACTS_PATH = MAIN_WORKSPACE_PATH + ARTIFACTS_PATH;
  private static final String COUNTER_ARTIFACT_NAME = "c0";
  private static final String COUNTER_ARTIFACT_PATH = MAIN_ARTIFACTS_PATH + COUNTER_ARTIFACT_NAME;
  private static final String CALLBACK_IRI = "http://localhost:8080/callback";
  private static final String NAMES_EQUAL_MESSAGE = "The names should be equal";
  private static final String URIS_EQUAL_MESSAGE = "The URIs should be equal";
  private static final String TDS_EQUAL_MESSAGE = "The thing descriptions should be equal";
  private static final String OK_STATUS_MESSAGE = "Status code should be OK";
  private static final String CREATED_STATUS_MESSAGE = "Status code should be CREATED";
  private static final String RESPONSE_BODY_STATUS_CODE_MESSAGE =
      "The response body should contain the status code description";
  private static final String INTERNAL_SERVER_ERROR_STATUS_MESSAGE =
      "The status code should be INTERNAL SERVER ERROR";

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
  public void testPostSubWorkspaceSucceeds(final VertxTestContext ctx) {
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
                INTERNAL_SERVER_ERROR_STATUS_MESSAGE
            );
            Assertions.assertEquals(
                "Internal Server Error",
                r.bodyAsString(),
                RESPONSE_BODY_STATUS_CODE_MESSAGE
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

  @Test
  public void testPostArtifactSucceeds(final VertxTestContext ctx) {
    try {
      final var expectedArtifactRepresentation =
          Files.readString(
            Path.of(ClassLoader.getSystemResource("c0_counter_artifact_td.ttl").toURI())
          );
      final var artifactInitialization =
          JsonObject.of(
            "artifactName",
            COUNTER_ARTIFACT_NAME,
            "artifactClass",
            "org.hyperagents.yggdrasil.cartago.artifacts.Counter",
            "initParams",
            JsonArray.of(5)
          );
      final var request = this.client.post(TEST_PORT, TEST_HOST, MAIN_ARTIFACTS_PATH)
                                     .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                     .putHeader(
                                       HttpHeaders.CONTENT_TYPE.toString(),
                                       ContentType.APPLICATION_JSON.getMimeType()
                                     )
                                     .sendBuffer(artifactInitialization.toBuffer());
      final var cartagoMessage = this.cartagoMessageQueue.take();
      final var createArtifactMessage =
          (CartagoMessage.CreateArtifact) cartagoMessage.body();
      Assertions.assertEquals(
          COUNTER_ARTIFACT_NAME,
          createArtifactMessage.artifactName(),
          NAMES_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          MAIN_WORKSPACE_NAME,
          createArtifactMessage.workspaceName(),
          NAMES_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          artifactInitialization,
          Json.decodeValue(createArtifactMessage.representation()),
          "The initialization parameters should be the same"
      );
      Assertions.assertEquals(
          TEST_AGENT_ID,
          createArtifactMessage.agentId(),
          NAMES_EQUAL_MESSAGE
      );
      cartagoMessage.reply(expectedArtifactRepresentation);
      final var storeMessage = this.storeMessageQueue.take();
      final var createEntityMessage = (RdfStoreMessage.CreateEntity) storeMessage.body();
      Assertions.assertEquals(
          COUNTER_ARTIFACT_NAME,
          createEntityMessage.entityName(),
          NAMES_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          "http://" + TEST_HOST + ":" + TEST_PORT + MAIN_ARTIFACTS_PATH,
          createEntityMessage.requestUri(),
          URIS_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          expectedArtifactRepresentation,
          createEntityMessage.entityRepresentation(),
          TDS_EQUAL_MESSAGE
      );
      storeMessage.reply(expectedArtifactRepresentation);
      request
          .onSuccess(r -> {
            Assertions.assertEquals(
                HttpStatus.SC_CREATED,
                r.statusCode(),
                CREATED_STATUS_MESSAGE
            );
            Assertions.assertEquals(
                expectedArtifactRepresentation,
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
  public void testPostArtifactWithWorkspaceNotFound(final VertxTestContext ctx) {
    try {
      final var artifactInitialization =
          JsonObject.of(
            "artifactName",
            COUNTER_ARTIFACT_NAME,
            "artifactClass",
            "org.hyperagents.yggdrasil.cartago.artifacts.Counter",
            "initParams",
            JsonArray.of(5)
          );
      final var request = this.client.post(
                                       TEST_PORT,
                                       TEST_HOST,
                                       WORKSPACES_PATH + "nonexistent" + ARTIFACTS_PATH
                                     )
                                     .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                     .putHeader(SLUG_HEADER, COUNTER_ARTIFACT_NAME)
                                     .putHeader(
                                       HttpHeaders.CONTENT_TYPE.toString(),
                                       ContentType.APPLICATION_JSON.getMimeType()
                                     )
                                     .sendBuffer(artifactInitialization.toBuffer());
      final var message = this.cartagoMessageQueue.take();
      final var createArtifactMessage = (CartagoMessage.CreateArtifact) message.body();
      Assertions.assertEquals(
          "nonexistent",
          createArtifactMessage.workspaceName(),
          NAMES_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          COUNTER_ARTIFACT_NAME,
          createArtifactMessage.artifactName(),
          NAMES_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          TEST_AGENT_ID,
          createArtifactMessage.agentId(),
          NAMES_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          artifactInitialization,
          Json.decodeValue(createArtifactMessage.representation()),
          "The initialization parameters should be the same"
      );
      message.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR, "The workspace was not found");
      request
          .onSuccess(r -> {
            Assertions.assertEquals(
                HttpStatus.SC_INTERNAL_SERVER_ERROR,
                r.statusCode(),
                INTERNAL_SERVER_ERROR_STATUS_MESSAGE
            );
            Assertions.assertEquals(
                "Internal Server Error",
                r.bodyAsString(),
                RESPONSE_BODY_STATUS_CODE_MESSAGE
            );
          })
          .onComplete(ctx.succeedingThenComplete());
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostArtifactFailsWithoutWebId(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestFailsWithoutWebId(
          ctx,
          this.client.post(TEST_PORT, TEST_HOST, MAIN_ARTIFACTS_PATH)
                     .putHeader(
                       HttpHeaders.CONTENT_TYPE.toString(),
                       ContentType.APPLICATION_JSON.getMimeType()
                     )
                     .sendBuffer(
                       JsonObject
                         .of(
                           "artifactName",
                           COUNTER_ARTIFACT_NAME,
                           "artifactClass",
                           "org.hyperagents.yggdrasil.cartago.artifacts.Counter",
                           "initParams",
                           JsonArray.of(5)
                         )
                         .toBuffer()
                     )
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostArtifactFailsWithoutContentType(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestFailsWithoutContentType(
          ctx,
          HttpMethod.POST,
          MAIN_ARTIFACTS_PATH,
          JsonObject
              .of(
                "artifactName",
                COUNTER_ARTIFACT_NAME,
                "artifactClass",
                "org.hyperagents.yggdrasil.cartago.artifacts.Counter",
                "initParams",
                JsonArray.of(5)
              )
              .toBuffer()
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostJoinWorkspaceSucceeds(final VertxTestContext ctx) {
    try {
      final var request = this.client.post(TEST_PORT, TEST_HOST, MAIN_WORKSPACE_PATH + "/join")
                                     .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                     .send();
      final var cartagoMessage = this.cartagoMessageQueue.take();
      final var joinWorkspaceMessage = (CartagoMessage.JoinWorkspace) cartagoMessage.body();
      Assertions.assertEquals(
          TEST_AGENT_ID,
          joinWorkspaceMessage.agentId(),
          NAMES_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          MAIN_WORKSPACE_NAME,
          joinWorkspaceMessage.workspaceName(),
          NAMES_EQUAL_MESSAGE
      );
      cartagoMessage.reply(String.valueOf(HttpStatus.SC_OK));
      request
          .onSuccess(r -> {
            Assertions.assertEquals(
                HttpStatus.SC_OK,
                r.statusCode(),
                OK_STATUS_MESSAGE
            );
            Assertions.assertEquals(
                String.valueOf(HttpStatus.SC_OK),
                r.bodyAsString(),
                RESPONSE_BODY_STATUS_CODE_MESSAGE
            );
          })
          .onComplete(ctx.succeedingThenComplete());
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostJoinWorkspaceFails(final VertxTestContext ctx) {
    try {
      final var request = this.client.post(TEST_PORT, TEST_HOST, MAIN_WORKSPACE_PATH + "/join")
                                     .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                     .send();
      final var message = this.cartagoMessageQueue.take();
      final var joinWorkspaceMessage = (CartagoMessage.JoinWorkspace) message.body();
      Assertions.assertEquals(
          MAIN_WORKSPACE_NAME,
          joinWorkspaceMessage.workspaceName(),
          NAMES_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          TEST_AGENT_ID,
          joinWorkspaceMessage.agentId(),
          NAMES_EQUAL_MESSAGE
      );
      message.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR, "An error occurred");
      request
          .onSuccess(r -> {
            Assertions.assertEquals(
                HttpStatus.SC_INTERNAL_SERVER_ERROR,
                r.statusCode(),
                INTERNAL_SERVER_ERROR_STATUS_MESSAGE
            );
            Assertions.assertEquals(
                "Internal Server Error",
                r.bodyAsString(),
                RESPONSE_BODY_STATUS_CODE_MESSAGE
            );
          })
          .onComplete(ctx.succeedingThenComplete());
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostJoinWorkspaceFailsWithoutWebId(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestFailsWithoutWebId(
          ctx,
          this.client.post(TEST_PORT, TEST_HOST, MAIN_WORKSPACE_PATH + "/join").send()
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostJoinWorkspaceRedirectsWithSlash(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestRedirectsWithAddedSlash(
          ctx,
          HttpMethod.POST,
          MAIN_WORKSPACE_PATH + "/join/"
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostLeaveWorkspaceSucceeds(final VertxTestContext ctx) {
    try {
      final var request = this.client.post(TEST_PORT, TEST_HOST, MAIN_WORKSPACE_PATH + "/leave")
                                     .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                     .send();
      final var cartagoMessage = this.cartagoMessageQueue.take();
      final var leaveWorkspaceMessage = (CartagoMessage.LeaveWorkspace) cartagoMessage.body();
      Assertions.assertEquals(
          TEST_AGENT_ID,
          leaveWorkspaceMessage.agentId(),
          NAMES_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          MAIN_WORKSPACE_NAME,
          leaveWorkspaceMessage.workspaceName(),
          NAMES_EQUAL_MESSAGE
      );
      cartagoMessage.reply(String.valueOf(HttpStatus.SC_OK));
      request
          .onSuccess(r -> {
            Assertions.assertEquals(
                HttpStatus.SC_OK,
                r.statusCode(),
                OK_STATUS_MESSAGE
            );
            Assertions.assertEquals(
                String.valueOf(HttpStatus.SC_OK),
                r.bodyAsString(),
                RESPONSE_BODY_STATUS_CODE_MESSAGE
            );
          })
          .onComplete(ctx.succeedingThenComplete());
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostLeaveWorkspaceFails(final VertxTestContext ctx) {
    try {
      final var request = this.client.post(TEST_PORT, TEST_HOST, MAIN_WORKSPACE_PATH + "/leave")
                                     .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                     .send();
      final var message = this.cartagoMessageQueue.take();
      final var leaveWorkspaceMessage = (CartagoMessage.LeaveWorkspace) message.body();
      Assertions.assertEquals(
          MAIN_WORKSPACE_NAME,
          leaveWorkspaceMessage.workspaceName(),
          NAMES_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          TEST_AGENT_ID,
          leaveWorkspaceMessage.agentId(),
          NAMES_EQUAL_MESSAGE
      );
      message.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR, "An error occurred");
      request
          .onSuccess(r -> {
            Assertions.assertEquals(
                HttpStatus.SC_INTERNAL_SERVER_ERROR,
                r.statusCode(),
                INTERNAL_SERVER_ERROR_STATUS_MESSAGE
            );
            Assertions.assertEquals(
                "Internal Server Error",
                r.bodyAsString(),
                RESPONSE_BODY_STATUS_CODE_MESSAGE
            );
          })
          .onComplete(ctx.succeedingThenComplete());
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostLeaveWorkspaceFailsWithoutWebId(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestFailsWithoutWebId(
          ctx,
          this.client.post(TEST_PORT, TEST_HOST, MAIN_WORKSPACE_PATH + "/leave").send()
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostLeaveWorkspaceRedirectsWithSlash(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestRedirectsWithAddedSlash(
          ctx,
          HttpMethod.POST,
          MAIN_WORKSPACE_PATH + "/leave/"
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostFocusArtifactSucceeds(final VertxTestContext ctx) {
    try {
      final var request = this.client.post(TEST_PORT, TEST_HOST, MAIN_WORKSPACE_PATH + "/focus")
                                     .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                     .putHeader(
                                       HttpHeaders.CONTENT_TYPE.toString(),
                                       ContentType.APPLICATION_JSON.getMimeType()
                                     )
                                     .sendBuffer(JsonObject.of(
                                       "artifactName",
                                       COUNTER_ARTIFACT_NAME,
                                       "callbackIri",
                                       CALLBACK_IRI
                                     ).toBuffer());
      final var cartagoMessage = this.cartagoMessageQueue.take();
      final var leaveWorkspaceMessage = (CartagoMessage.Focus) cartagoMessage.body();
      Assertions.assertEquals(
          TEST_AGENT_ID,
          leaveWorkspaceMessage.agentId(),
          NAMES_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          MAIN_WORKSPACE_NAME,
          leaveWorkspaceMessage.workspaceName(),
          NAMES_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          COUNTER_ARTIFACT_NAME,
          leaveWorkspaceMessage.artifactName(),
          NAMES_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          CALLBACK_IRI,
          leaveWorkspaceMessage.callbackIri(),
          URIS_EQUAL_MESSAGE
      );
      cartagoMessage.reply(String.valueOf(HttpStatus.SC_OK));
      request
          .onSuccess(r -> {
            Assertions.assertEquals(
                HttpStatus.SC_OK,
                r.statusCode(),
                OK_STATUS_MESSAGE
            );
            Assertions.assertEquals(
                String.valueOf(HttpStatus.SC_OK),
                r.bodyAsString(),
                RESPONSE_BODY_STATUS_CODE_MESSAGE
            );
          })
          .onComplete(ctx.succeedingThenComplete());
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostFocusArtifactFails(final VertxTestContext ctx) {
    try {
      final var request = this.client.post(TEST_PORT, TEST_HOST, MAIN_WORKSPACE_PATH + "/focus")
                                     .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                     .putHeader(
                                       HttpHeaders.CONTENT_TYPE.toString(),
                                       ContentType.APPLICATION_JSON.getMimeType()
                                     )
                                     .sendBuffer(JsonObject.of(
                                       "artifactName",
                                       "c1",
                                       "callbackIri",
                                       CALLBACK_IRI
                                     ).toBuffer());
      final var message = this.cartagoMessageQueue.take();
      final var leaveWorkspaceMessage = (CartagoMessage.Focus) message.body();
      Assertions.assertEquals(
          TEST_AGENT_ID,
          leaveWorkspaceMessage.agentId(),
          NAMES_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          MAIN_WORKSPACE_NAME,
          leaveWorkspaceMessage.workspaceName(),
          NAMES_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          "c1",
          leaveWorkspaceMessage.artifactName(),
          NAMES_EQUAL_MESSAGE
      );
      Assertions.assertEquals(
          CALLBACK_IRI,
          leaveWorkspaceMessage.callbackIri(),
          URIS_EQUAL_MESSAGE
      );
      message.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR, "An error occurred");
      request
          .onSuccess(r -> {
            Assertions.assertEquals(
                HttpStatus.SC_INTERNAL_SERVER_ERROR,
                r.statusCode(),
                INTERNAL_SERVER_ERROR_STATUS_MESSAGE
            );
            Assertions.assertEquals(
                "Internal Server Error",
                r.bodyAsString(),
                RESPONSE_BODY_STATUS_CODE_MESSAGE
            );
          })
          .onComplete(ctx.succeedingThenComplete());
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostFocusArtifactFailsWithoutWebId(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestFailsWithoutWebId(
          ctx,
          this.client.post(TEST_PORT, TEST_HOST, MAIN_WORKSPACE_PATH + "/focus")
                     .putHeader(
                       HttpHeaders.CONTENT_TYPE.toString(),
                       ContentType.APPLICATION_JSON.getMimeType()
                     )
                     .sendBuffer(JsonObject.of(
                       "artifactName",
                       COUNTER_ARTIFACT_NAME,
                       "callbackIri",
                       CALLBACK_IRI
                     ).toBuffer())
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostFocusArtifactRedirectsWithSlash(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestRedirectsWithAddedSlash(
          ctx,
          HttpMethod.POST,
          MAIN_WORKSPACE_PATH + "/focus/"
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostFocusWorkspaceFailsWithoutContentType(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestFailsWithoutContentType(
          ctx,
          HttpMethod.POST,
          MAIN_WORKSPACE_PATH + "/focus",
          JsonObject
              .of(
                "artifactName",
                COUNTER_ARTIFACT_NAME,
                "callbackIri",
                CALLBACK_IRI
              )
              .toBuffer()
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }
}
