package org.hyperagents.yggdrasil.http;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.eventbus.messageboxes.CartagoMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.WebSubConfig;
import org.hyperagents.yggdrasil.utils.impl.EnvironmentConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.WebSubConfigImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@ExtendWith(VertxExtension.class)
public class CartagoArtifactActionHandlerTest {
  private static final int TEST_PORT = 8080;
  private static final String TEST_HOST = "localhost";
  private static final String AGENT_WEBID = "X-Agent-WebID";
  private static final String TEST_AGENT_ID = "test_agent";
  private static final String MAIN_WORKSPACE_NAME = "test";
  private static final String MAIN_ARTIFACTS_PATH =
      "/workspaces/" + MAIN_WORKSPACE_NAME + "/artifacts/";
  private static final String COUNTER_ARTIFACT_NAME = "c0";
  private static final String COUNTER_ARTIFACT_PATH = MAIN_ARTIFACTS_PATH + COUNTER_ARTIFACT_NAME;
  private static final String INCREMENT_ACTION_NAME = "inc";
  private static final String ADDER_ARTIFACT_NAME = "a0";
  private static final String ADDER_ARTIFACT_PATH = MAIN_ARTIFACTS_PATH + ADDER_ARTIFACT_NAME;
  private static final String ADD_ACTION_NAME = "add";
  private static final List<Object> ADD_PARAMS = List.of(2, 3);
  private static final String NAMES_EQUAL_MESSAGE = "The names should be equal";
  private static final String CONTENTS_EQUAL_MESSAGE = "The contents should be equal";
  private static final String URIS_EQUAL_MESSAGE = "The URIs should be equal";
  private static final String OK_STATUS_MESSAGE = "Status code should be OK";
  private static final String INTERNAL_SERVER_ERROR_STATUS_MESSAGE =
      "The status code should be INTERNAL SERVER ERROR";

  private final BlockingQueue<Message<RdfStoreMessage>> storeMessageQueue;
  private final BlockingQueue<Message<CartagoMessage>> cartagoMessageQueue;
  private WebClient client;
  private HttpServerVerticleTestHelper helper;

  public CartagoArtifactActionHandlerTest() {
    this.storeMessageQueue = new LinkedBlockingQueue<>();
    this.cartagoMessageQueue = new LinkedBlockingQueue<>();
  }

  @BeforeEach
  public void setUp(final Vertx vertx, final VertxTestContext ctx,final TestInfo testInfo) {
    final String ontology;
    final String testName = testInfo.getTestMethod().orElseThrow().getName();
    if (testName.contains("TD")) {
      ontology = "td";
    } else if (testName.contains("HMAS")) {
      ontology = "hmas";
    } else {
      throw new RuntimeException("Test did not speficy ontology");
    }

    this.client = WebClient.create(vertx);
    this.helper = new HttpServerVerticleTestHelper(this.client, this.storeMessageQueue);
    final var httpConfig = new HttpInterfaceConfigImpl(JsonObject.of());
    vertx.sharedData()
         .<String, HttpInterfaceConfig>getLocalMap("http-config")
         .put("default", httpConfig);
    final var environmentConfig = new EnvironmentConfigImpl(JsonObject.of(
        "environment-config",
        JsonObject.of("enabled", true, "ontology",ontology
        )
    ));
    vertx.sharedData()
         .<String, EnvironmentConfig>getLocalMap("environment-config")
         .put("default", environmentConfig);
    final var notificationConfig = new WebSubConfigImpl(
        JsonObject.of(
          "notification-config",
          JsonObject.of("enabled", true)
        ),
        httpConfig
    );
    vertx.sharedData()
         .<String, WebSubConfig>getLocalMap("notification-config")
         .put("default", notificationConfig);
    final var storeMessagebox = new RdfStoreMessagebox(vertx.eventBus());
    storeMessagebox.init();
    storeMessagebox.receiveMessages(this.storeMessageQueue::add);
    final var cartagoMessagebox = new CartagoMessagebox(vertx.eventBus(), environmentConfig);
    cartagoMessagebox.init();
    cartagoMessagebox.receiveMessages(this.cartagoMessageQueue::add);
    new HttpNotificationDispatcherMessagebox(vertx.eventBus(), notificationConfig).init();
    vertx.deployVerticle(new HttpServerVerticle(), ctx.succeedingThenComplete());
  }

  @AfterEach
  public void tearDown(final Vertx vertx, final VertxTestContext ctx) {
    vertx.close(ctx.succeedingThenComplete());
  }

  @Test
  public void testPostArtifactActionSucceedsHMAS(final VertxTestContext ctx)
    throws URISyntaxException, IOException, InterruptedException {

    final var artifactRepresentation = Files.readString(
      Path.of(ClassLoader.getSystemResource("a0_adder_artifact_hmas.ttl").toURI()),
      StandardCharsets.UTF_8
    );
    final var request = this.client.post(
        TEST_PORT,
        TEST_HOST,
        ADDER_ARTIFACT_PATH + "/" + ADD_ACTION_NAME
      )
      .putHeader(AGENT_WEBID, TEST_AGENT_ID)
      .sendJson(ADD_PARAMS);




    final var storeMessage = this.storeMessageQueue.take();
    final var getEntityMessage = (RdfStoreMessage.GetEntity) storeMessage.body();
    Assertions.assertEquals(
      this.helper.getUri(ADDER_ARTIFACT_PATH),
      getEntityMessage.requestUri(),
      URIS_EQUAL_MESSAGE
    );
    storeMessage.reply(artifactRepresentation);
    final var cartagoMessage = this.cartagoMessageQueue.take();
    final var doActionMessage = (CartagoMessage.DoAction) cartagoMessage.body();
    Assertions.assertEquals(
      ADDER_ARTIFACT_NAME,
      doActionMessage.artifactName(),
      NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
      MAIN_WORKSPACE_NAME,
      doActionMessage.workspaceName(),
      NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
      "POSThttp://localhost:8080/workspaces/test/artifacts/a0/add",
      doActionMessage.actionName(),
      NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
      TEST_AGENT_ID,
      doActionMessage.agentId(),
      NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
      Optional.of("[2,3]"),
      Optional.of(doActionMessage.context()),
      CONTENTS_EQUAL_MESSAGE
    );
    cartagoMessage.reply(String.valueOf(5));
    request
      .onSuccess(r -> {
        Assertions.assertEquals(
          HttpStatus.SC_OK,
          r.statusCode(),
          OK_STATUS_MESSAGE
        );
        Assertions.assertEquals(
          "[5]",
          r.bodyAsString(),
          "The response bodies should be equal"
        );
      })
      .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testPostArtifactActionSucceedsTD(final VertxTestContext ctx)
      throws URISyntaxException, IOException, InterruptedException {

    final var artifactRepresentation = Files.readString(
        Path.of(ClassLoader.getSystemResource("a0_adder_artifact.ttl").toURI()),
        StandardCharsets.UTF_8
    );
    final var request = this.client.post(
                                     TEST_PORT,
                                     TEST_HOST,
                                     ADDER_ARTIFACT_PATH + "/" + ADD_ACTION_NAME
                                   )
                                   .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                   .sendJson(ADD_PARAMS);




    final var storeMessage = this.storeMessageQueue.take();
    final var getEntityMessage = (RdfStoreMessage.GetEntity) storeMessage.body();
    Assertions.assertEquals(
        this.helper.getUri(ADDER_ARTIFACT_PATH),
        getEntityMessage.requestUri(),
        URIS_EQUAL_MESSAGE
    );
    storeMessage.reply(artifactRepresentation);
    final var cartagoMessage = this.cartagoMessageQueue.take();
    final var doActionMessage = (CartagoMessage.DoAction) cartagoMessage.body();
    Assertions.assertEquals(
        ADDER_ARTIFACT_NAME,
        doActionMessage.artifactName(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        MAIN_WORKSPACE_NAME,
        doActionMessage.workspaceName(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        "POSThttp://localhost:8080/workspaces/test/artifacts/a0/add",
        doActionMessage.actionName(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        TEST_AGENT_ID,
        doActionMessage.agentId(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        Optional.of("[2,3]"),
        Optional.of(doActionMessage.context()),
        CONTENTS_EQUAL_MESSAGE
    );
    cartagoMessage.reply(String.valueOf(5));
    request
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );
          Assertions.assertEquals(
              "[5]",
              r.bodyAsString(),
              "The response bodies should be equal"
          );
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testPostArtifactActionWithoutFeedbackSucceedsTD(final VertxTestContext ctx)
      throws InterruptedException, URISyntaxException, IOException {
    final var artifactRepresentation = Files.readString(
        Path.of(ClassLoader.getSystemResource("c0_counter_artifact_td.ttl").toURI()),
        StandardCharsets.UTF_8
    );
    final var request = this.client.post(
                                     TEST_PORT,
                                     TEST_HOST,
                                     COUNTER_ARTIFACT_PATH + "/" + INCREMENT_ACTION_NAME
                                   )
                                   .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                   .send();
    final var storeMessage = this.storeMessageQueue.take();
    final var getEntityMessage = (RdfStoreMessage.GetEntity) storeMessage.body();
    Assertions.assertEquals(
        this.helper.getUri(COUNTER_ARTIFACT_PATH),
        getEntityMessage.requestUri(),
        URIS_EQUAL_MESSAGE
    );
    storeMessage.reply(artifactRepresentation);
    final var cartagoMessage = this.cartagoMessageQueue.take();
    final var doActionMessage = (CartagoMessage.DoAction) cartagoMessage.body();
    Assertions.assertEquals(
        COUNTER_ARTIFACT_NAME,
        doActionMessage.artifactName(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        MAIN_WORKSPACE_NAME,
        doActionMessage.workspaceName(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        "POSThttp://localhost:8080/workspaces/test/artifacts/c0/inc",
        doActionMessage.actionName(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        TEST_AGENT_ID,
        doActionMessage.agentId(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertNull(doActionMessage.context(), CONTENTS_EQUAL_MESSAGE);
    cartagoMessage.reply(null);
    request
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );
          Assertions.assertNull(
              r.bodyAsString(),
              "The response body should be empty"
          );
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testPostArtifactActionFailsWithWrongArtifactUriTD(final VertxTestContext ctx)
      throws InterruptedException {
    final var wrongUri = MAIN_ARTIFACTS_PATH + "nonexistent";
    final var request = this.client.post(TEST_PORT, TEST_HOST, wrongUri + "/" + ADD_ACTION_NAME)
                                   .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                   .sendJson(ADD_PARAMS);
    final var storeMessage = this.storeMessageQueue.take();
    final var getEntityMessage = (RdfStoreMessage.GetEntity) storeMessage.body();
    Assertions.assertEquals(
        this.helper.getUri(wrongUri),
        getEntityMessage.requestUri(),
        URIS_EQUAL_MESSAGE
    );
    storeMessage.fail(HttpStatus.SC_NOT_FOUND, "The requested entity was not found");
    request
        .onSuccess(r -> Assertions.assertEquals(
            HttpStatus.SC_INTERNAL_SERVER_ERROR,
            r.statusCode(),
            INTERNAL_SERVER_ERROR_STATUS_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  // TODO: Should be tested in the cartagoVerticle makes more sense
  @Test
  public void testPostArtifactActionFailsWithWrongActionNameTD(final VertxTestContext ctx)
      throws URISyntaxException, IOException, InterruptedException {
    final var artifactRepresentation = Files.readString(
        Path.of(ClassLoader.getSystemResource("a0_adder_artifact.ttl").toURI()),
        StandardCharsets.UTF_8
    );
    final var request = this.client.post(
                                     TEST_PORT,
                                     TEST_HOST,
                                     ADDER_ARTIFACT_PATH + "/" + INCREMENT_ACTION_NAME
                                   )
                                   .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                   .sendJson(ADD_PARAMS);
    final var storeMessage = this.storeMessageQueue.take();
    final var getEntityMessage = (RdfStoreMessage.GetEntity) storeMessage.body();
    Assertions.assertEquals(
        this.helper.getUri(ADDER_ARTIFACT_PATH),
        getEntityMessage.requestUri(),
        URIS_EQUAL_MESSAGE
    );
    storeMessage.reply(artifactRepresentation);
    final var cartagoMessage = this.cartagoMessageQueue.take();
    final var doActionMessage = (CartagoMessage.DoAction) cartagoMessage.body();
    Assertions.assertEquals(
        ADDER_ARTIFACT_NAME,
        doActionMessage.artifactName(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        MAIN_WORKSPACE_NAME,
        doActionMessage.workspaceName(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
      "POSThttp://localhost:8080/workspaces/test/artifacts/a0/inc",
      doActionMessage.actionName(),
      NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        TEST_AGENT_ID,
        doActionMessage.agentId(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        "[2,3]",
        doActionMessage.context(),
        CONTENTS_EQUAL_MESSAGE
    );
    cartagoMessage.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR, "An error has occurred.");
    request
        .onSuccess(r -> Assertions.assertEquals(
            HttpStatus.SC_INTERNAL_SERVER_ERROR,
            r.statusCode(),
            INTERNAL_SERVER_ERROR_STATUS_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testPostArtifactActionFailsWithoutWebIdTD(final VertxTestContext ctx) {
    this.helper.testResourceRequestFailsWithoutWebId(
        ctx,
        this.client.post(TEST_PORT, TEST_HOST, COUNTER_ARTIFACT_PATH + "/" + INCREMENT_ACTION_NAME)
                   .send()
    );
  }
}
