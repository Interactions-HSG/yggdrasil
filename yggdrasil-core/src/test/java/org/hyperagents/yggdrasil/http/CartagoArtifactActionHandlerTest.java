package org.hyperagents.yggdrasil.http;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.cartago.CartagoDataBundle;
import org.hyperagents.yggdrasil.eventbus.messageboxes.CartagoMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class CartagoArtifactActionHandlerTest {
  private static final int TEST_PORT = 8080;
  private static final String TEST_HOST = "localhost";
  private static final String AGENT_WEBID = "X-Agent-WebID";
  private static final String TEST_AGENT_ID = "test_agent";
  private static final String MAIN_WORKSPACE_NAME = "test";
  private static final String WORKSPACES_PATH = "/workspaces/";
  private static final String MAIN_WORKSPACE_PATH = WORKSPACES_PATH + MAIN_WORKSPACE_NAME;
  private static final String ARTIFACTS_PATH = "/artifacts/";
  private static final String MAIN_ARTIFACTS_PATH = MAIN_WORKSPACE_PATH + ARTIFACTS_PATH;
  private static final String COUNTER_ARTIFACT_NAME = "c0";
  private static final String COUNTER_ARTIFACT_PATH = MAIN_ARTIFACTS_PATH + COUNTER_ARTIFACT_NAME;
  private static final String INCREMENT_ACTION_NAME = "inc";
  private static final String ADDER_ARTIFACT_NAME = "a0";
  private static final String ADDER_ARTIFACT_PATH = MAIN_ARTIFACTS_PATH + ADDER_ARTIFACT_NAME;
  private static final String ADD_ACTION_NAME = "add";
  private static final List<Object> ADD_PARAMS = List.of(2, 3);
  private static final String NAMES_EQUAL_MESSAGE = "The names should be equal";
  private static final String CONTENTS_EQUAL_MESSAGE = "The contents should be equal";
  private static final String OK_STATUS_MESSAGE = "Status code should be OK";
  private static final String RESPONSE_BODY_STATUS_CODE_MESSAGE =
      "The response body should contain the status code description";
  private static final String INTERNAL_SERVER_ERROR_STATUS_MESSAGE =
      "The status code should be INTERNAL SERVER ERROR";
  public static final String RESPONSE_BODY_EMPTY_MESSAGE = "The response body should be empty";

  private final BlockingQueue<Message<RdfStoreMessage>> storeMessageQueue;
  private final BlockingQueue<Message<CartagoMessage>> cartagoMessageQueue;
  private WebClient client;
  private HttpServerVerticleTestHelper helper;

  public CartagoArtifactActionHandlerTest() {
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
  @Disabled
  public void testPostArtifactActionSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException, InterruptedException {
    final var artifactRepresentation = Files.readString(Path.of(
        ClassLoader.getSystemResource("a0_adder_artifact.ttl").toURI()
    ));
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
        "http://" + TEST_HOST + ":" + TEST_PORT + ADDER_ARTIFACT_PATH,
        getEntityMessage.requestUri()
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
        ADD_ACTION_NAME,
        doActionMessage.actionName(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        TEST_AGENT_ID,
        doActionMessage.agentId(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        Optional.of(CartagoDataBundle.toJson(ADD_PARAMS)),
        doActionMessage.content(),
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
              String.valueOf(5),
              r.bodyAsString(),
              "The response bodies should be equal"
          );
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  @Disabled
  public void testPostArtifactActionWithoutFeedbackSucceeds(final VertxTestContext ctx)
      throws InterruptedException, URISyntaxException, IOException {
    final var artifactRepresentation = Files.readString(Path.of(
        ClassLoader.getSystemResource("c0_counter_artifact_td.ttl").toURI()
    ));
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
        "http://" + TEST_HOST + ":" + TEST_PORT + COUNTER_ARTIFACT_PATH,
        getEntityMessage.requestUri()
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
        INCREMENT_ACTION_NAME,
        doActionMessage.actionName(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        TEST_AGENT_ID,
        doActionMessage.agentId(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        Optional.empty(),
        doActionMessage.content(),
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
          Assertions.assertNull(
              r.bodyAsString(),
              RESPONSE_BODY_EMPTY_MESSAGE
          );
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testPostArtifactActionFailsWithWrongArtifactUri(final VertxTestContext ctx)
      throws InterruptedException {
    final var wrongUri = MAIN_ARTIFACTS_PATH + "nonexistent";
    final var request = this.client.post(TEST_PORT, TEST_HOST, wrongUri + "/" + ADD_ACTION_NAME)
                                   .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                                   .sendJson(ADD_PARAMS);
    final var storeMessage = this.storeMessageQueue.take();
    final var getEntityMessage = (RdfStoreMessage.GetEntity) storeMessage.body();
    Assertions.assertEquals(
        "http://" + TEST_HOST + ":" + TEST_PORT + wrongUri,
        getEntityMessage.requestUri()
    );
    storeMessage.fail(HttpStatus.SC_NOT_FOUND, "The requested entity was not found");
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
  }

  @Test
  public void testPostArtifactActionFailsWithWrongActionName(final VertxTestContext ctx)
      throws URISyntaxException, IOException, InterruptedException {
    final var artifactRepresentation = Files.readString(Path.of(
        ClassLoader.getSystemResource("a0_adder_artifact.ttl").toURI()
    ));
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
        "http://" + TEST_HOST + ":" + TEST_PORT + ADDER_ARTIFACT_PATH,
        getEntityMessage.requestUri()
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
    Assertions.assertNull(
        doActionMessage.actionName(),
        "The action name should be null"
    );
    Assertions.assertEquals(
        TEST_AGENT_ID,
        doActionMessage.agentId(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        Optional.empty(),
        doActionMessage.content(),
        CONTENTS_EQUAL_MESSAGE
    );
    cartagoMessage.fail(HttpStatus.SC_INTERNAL_SERVER_ERROR, "An error has occurred.");
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
  }

  @Test
  public void testPostArtifactActionFailsWithoutWebId(final VertxTestContext ctx) {
    this.helper.testResourceRequestFailsWithoutWebId(
        ctx,
        this.client.post(TEST_PORT, TEST_HOST, COUNTER_ARTIFACT_PATH + "/" + INCREMENT_ACTION_NAME)
                   .send()
    );
  }
}
