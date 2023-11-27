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
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
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

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
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
  private static final String FOCUS_PATH = MAIN_WORKSPACE_PATH + "/focus";
  private static final String ARTIFACTS_PATH = "/artifacts/";
  private static final String MAIN_ARTIFACTS_PATH = MAIN_WORKSPACE_PATH + ARTIFACTS_PATH;
  private static final String COUNTER_ARTIFACT_NAME = "c0";
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
  private static final String NONEXISTENT_NAME = "nonexistent";
  private static final String INTERNAL_SERVER_ERROR_BODY = "Internal Server Error";
  private static final String ARTIFACT_NAME_PARAM = "artifactName";
  private static final String ARTIFACT_CLASS_PARAM = "artifactClass";
  private static final String INIT_PARAMS_PARAM = "initParams";
  private static final String CALLBACK_IRI_PARAM = "callbackIri";
  private static final String COUNTER_CLASS_NAME =
      "org.hyperagents.yggdrasil.cartago.artifacts.Counter";

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
  public void testPostWorkspacesSucceeds(final VertxTestContext ctx)
      throws InterruptedException, URISyntaxException, IOException {
    final var expectedWorkspaceRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()),
          StandardCharsets.UTF_8
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
    final var createEntityMessage = (RdfStoreMessage.CreateWorkspace) storeMessage.body();
    Assertions.assertEquals(
        MAIN_WORKSPACE_NAME,
        createEntityMessage.workspaceName(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        this.helper.getUri(WORKSPACES_PATH),
        createEntityMessage.requestUri(),
        URIS_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        Optional.empty(),
        createEntityMessage.parentWorkspaceUri(),
        "The parent URI should not be present"
    );
    Assertions.assertEquals(
        expectedWorkspaceRepresentation,
        createEntityMessage.workspaceRepresentation(),
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
  }

  @Test
  public void testPostWorkspacesFailsWithoutWebId(final VertxTestContext ctx) {
    this.helper.testResourceRequestFailsWithoutWebId(
        ctx,
        this.client.post(TEST_PORT, TEST_HOST, WORKSPACES_PATH)
                   .putHeader(SLUG_HEADER, MAIN_WORKSPACE_NAME)
                   .send()
    );
  }

  @Test
  public void testPostSubWorkspaceSucceeds(final VertxTestContext ctx)
      throws InterruptedException, URISyntaxException, IOException {
    final var expectedWorkspaceRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("sub_workspace_td.ttl").toURI()),
          StandardCharsets.UTF_8
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
    final var createEntityMessage = (RdfStoreMessage.CreateWorkspace) storeMessage.body();
    Assertions.assertEquals(
        SUB_WORKSPACE_NAME,
        createEntityMessage.workspaceName(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        this.helper.getUri(WORKSPACES_PATH),
        createEntityMessage.requestUri(),
        URIS_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        Optional.of(this.helper.getUri(MAIN_WORKSPACE_PATH)),
        createEntityMessage.parentWorkspaceUri(),
        "The parent workspace URI should be present"
    );
    Assertions.assertEquals(
        expectedWorkspaceRepresentation,
        createEntityMessage.workspaceRepresentation(),
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
  }

  @Test
  public void testPostSubWorkspaceWithParentNotFound(final VertxTestContext ctx)
      throws InterruptedException {
    final var request = this.client.post(TEST_PORT, TEST_HOST, WORKSPACES_PATH + NONEXISTENT_NAME)
                                   .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                   .putHeader(SLUG_HEADER, SUB_WORKSPACE_NAME)
                                   .send();
    final var message = this.cartagoMessageQueue.take();
    final var createSubWorkspaceMessage =
        (CartagoMessage.CreateSubWorkspace) message.body();
    Assertions.assertEquals(
        NONEXISTENT_NAME,
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
              INTERNAL_SERVER_ERROR_BODY,
              r.bodyAsString(),
              RESPONSE_BODY_STATUS_CODE_MESSAGE
          );
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testPostSubWorkspaceFailsWithoutWebId(final VertxTestContext ctx) {
    this.helper.testResourceRequestFailsWithoutWebId(
        ctx,
        this.client.post(TEST_PORT, TEST_HOST, MAIN_WORKSPACE_PATH)
                   .putHeader(SLUG_HEADER, SUB_WORKSPACE_NAME)
                   .send()
    );
  }

  @Test
  public void testPostSubWorkspaceRedirectsWithSlash(final VertxTestContext ctx) {
    this.helper.testResourceRequestRedirectsWithAddedSlash(
        ctx,
        HttpMethod.POST,
        MAIN_WORKSPACE_PATH
    );
  }

  @Test
  public void testPostArtifactSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException, InterruptedException {
    final var expectedArtifactRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("c0_counter_artifact_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    final var artifactInitialization =
        JsonObject.of(
          ARTIFACT_NAME_PARAM,
          COUNTER_ARTIFACT_NAME,
          ARTIFACT_CLASS_PARAM,
          COUNTER_CLASS_NAME,
          INIT_PARAMS_PARAM,
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
    final var createEntityMessage = (RdfStoreMessage.CreateArtifact) storeMessage.body();
    Assertions.assertEquals(
        COUNTER_ARTIFACT_NAME,
        createEntityMessage.artifactName(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        this.helper.getUri(MAIN_ARTIFACTS_PATH),
        createEntityMessage.requestUri(),
        URIS_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        expectedArtifactRepresentation,
        createEntityMessage.artifactRepresentation(),
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
  }

  @Test
  public void testPostArtifactWithWorkspaceNotFound(final VertxTestContext ctx)
      throws InterruptedException {
    final var artifactInitialization =
        JsonObject.of(
          ARTIFACT_NAME_PARAM,
          COUNTER_ARTIFACT_NAME,
          ARTIFACT_CLASS_PARAM,
          COUNTER_CLASS_NAME,
          INIT_PARAMS_PARAM,
          JsonArray.of(5)
        );
    final var request = this.client.post(
                                     TEST_PORT,
                                     TEST_HOST,
                                     WORKSPACES_PATH + NONEXISTENT_NAME + ARTIFACTS_PATH
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
        NONEXISTENT_NAME,
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
              INTERNAL_SERVER_ERROR_BODY,
              r.bodyAsString(),
              RESPONSE_BODY_STATUS_CODE_MESSAGE
          );
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testPostArtifactFailsWithoutWebId(final VertxTestContext ctx) {
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
                         ARTIFACT_NAME_PARAM,
                         COUNTER_ARTIFACT_NAME,
                         ARTIFACT_CLASS_PARAM,
                         COUNTER_CLASS_NAME,
                         INIT_PARAMS_PARAM,
                         JsonArray.of(5)
                       )
                       .toBuffer()
                   )
    );
  }

  @Test
  public void testPostArtifactFailsWithoutContentType(final VertxTestContext ctx) {
    this.helper.testResourceRequestFailsWithoutContentType(
        ctx,
        HttpMethod.POST,
        MAIN_ARTIFACTS_PATH,
        JsonObject
            .of(
              ARTIFACT_NAME_PARAM,
              COUNTER_ARTIFACT_NAME,
              ARTIFACT_CLASS_PARAM,
              COUNTER_CLASS_NAME,
              INIT_PARAMS_PARAM,
              JsonArray.of(5)
            )
            .toBuffer()
    );
  }

  @Test
  public void testPostJoinWorkspaceSucceeds(final VertxTestContext ctx)
      throws InterruptedException {
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
  }

  @Test
  public void testPostJoinWorkspaceFails(final VertxTestContext ctx)
      throws InterruptedException {
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
              INTERNAL_SERVER_ERROR_BODY,
              r.bodyAsString(),
              RESPONSE_BODY_STATUS_CODE_MESSAGE
          );
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testPostJoinWorkspaceFailsWithoutWebId(final VertxTestContext ctx) {
    this.helper.testResourceRequestFailsWithoutWebId(
        ctx,
        this.client.post(TEST_PORT, TEST_HOST, MAIN_WORKSPACE_PATH + "/join").send()
    );
  }

  @Test
  public void testPostJoinWorkspaceRedirectsWithSlash(final VertxTestContext ctx) {
    this.helper.testResourceRequestRedirectsWithAddedSlash(
        ctx,
        HttpMethod.POST,
        MAIN_WORKSPACE_PATH + "/join/"
    );
  }

  @Test
  public void testPostLeaveWorkspaceSucceeds(final VertxTestContext ctx)
      throws InterruptedException {
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
  }

  @Test
  public void testPostLeaveWorkspaceFails(final VertxTestContext ctx)
      throws InterruptedException {
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
              INTERNAL_SERVER_ERROR_BODY,
              r.bodyAsString(),
              RESPONSE_BODY_STATUS_CODE_MESSAGE
          );
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testPostLeaveWorkspaceFailsWithoutWebId(final VertxTestContext ctx) {
    this.helper.testResourceRequestFailsWithoutWebId(
        ctx,
        this.client.post(TEST_PORT, TEST_HOST, MAIN_WORKSPACE_PATH + "/leave").send()
    );
  }

  @Test
  public void testPostLeaveWorkspaceRedirectsWithSlash(final VertxTestContext ctx) {
    this.helper.testResourceRequestRedirectsWithAddedSlash(
        ctx,
        HttpMethod.POST,
        MAIN_WORKSPACE_PATH + "/leave/"
    );
  }

  @Test
  public void testPostFocusArtifactSucceeds(final VertxTestContext ctx)
      throws InterruptedException {
    final var request = this.client.post(TEST_PORT, TEST_HOST, FOCUS_PATH)
                                   .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                   .putHeader(
                                     HttpHeaders.CONTENT_TYPE.toString(),
                                     ContentType.APPLICATION_JSON.getMimeType()
                                   )
                                   .sendBuffer(JsonObject.of(
                                     ARTIFACT_NAME_PARAM,
                                     COUNTER_ARTIFACT_NAME,
                                     CALLBACK_IRI_PARAM,
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
  }

  @Test
  public void testPostFocusArtifactFails(final VertxTestContext ctx)
      throws InterruptedException {
    final var request = this.client.post(TEST_PORT, TEST_HOST, FOCUS_PATH)
                                   .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                   .putHeader(
                                     HttpHeaders.CONTENT_TYPE.toString(),
                                     ContentType.APPLICATION_JSON.getMimeType()
                                   )
                                   .sendBuffer(JsonObject.of(
                                     ARTIFACT_NAME_PARAM,
                                     "c1",
                                     CALLBACK_IRI_PARAM,
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
              INTERNAL_SERVER_ERROR_BODY,
              r.bodyAsString(),
              RESPONSE_BODY_STATUS_CODE_MESSAGE
          );
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testPostFocusArtifactFailsWithoutWebId(final VertxTestContext ctx) {
    this.helper.testResourceRequestFailsWithoutWebId(
        ctx,
        this.client.post(TEST_PORT, TEST_HOST, FOCUS_PATH)
                   .putHeader(
                     HttpHeaders.CONTENT_TYPE.toString(),
                     ContentType.APPLICATION_JSON.getMimeType()
                   )
                   .sendBuffer(JsonObject.of(
                     ARTIFACT_NAME_PARAM,
                     COUNTER_ARTIFACT_NAME,
                     CALLBACK_IRI_PARAM,
                     CALLBACK_IRI
                   ).toBuffer())
    );
  }

  @Test
  public void testPostFocusArtifactRedirectsWithSlash(final VertxTestContext ctx) {
    this.helper.testResourceRequestRedirectsWithAddedSlash(
        ctx,
        HttpMethod.POST,
        FOCUS_PATH + "/"
    );
  }

  @Test
  public void testPostFocusWorkspaceFailsWithoutContentType(final VertxTestContext ctx) {
    this.helper.testResourceRequestFailsWithoutContentType(
        ctx,
        HttpMethod.POST,
        FOCUS_PATH,
        JsonObject
            .of(
              ARTIFACT_NAME_PARAM,
              COUNTER_ARTIFACT_NAME,
              CALLBACK_IRI_PARAM,
              CALLBACK_IRI
            )
            .toBuffer()
    );
  }
}