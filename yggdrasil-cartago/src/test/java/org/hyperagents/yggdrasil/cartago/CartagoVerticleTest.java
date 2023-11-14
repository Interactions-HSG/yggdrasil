package org.hyperagents.yggdrasil.cartago;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.cartago.artifacts.Adder;
import org.hyperagents.yggdrasil.cartago.artifacts.Counter;
import org.hyperagents.yggdrasil.eventbus.messageboxes.CartagoMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
public class CartagoVerticleTest {
  private static final String MAIN_WORKSPACE_NAME = "test";
  private static final String SUB_WORKSPACE_NAME = "sub";
  private static final String AGENT_IRI = "http://localhost:8080/agents/test";
  private static final String FOCUSING_AGENT_IRI = "http://localhost:8080/agents/focusing_agent";
  private static final String ADDER_SEMANTIC_TYPE = "http://example.org/Adder";
  private static final String COUNTER_SEMANTIC_TYPE = "http://example.org/Counter";
  private static final String CALLBACK_IRI = "http://localhost:8080/callback";
  private static final String COUNTER_ARTIFACT_IRI = "http://localhost:8080/workspaces/test/artifacts/c0";
  private static final String NONEXISTENT_NAME = "nonexistent";
  private static final String ARTIFACT_SEMANTIC_TYPE_PARAM = "artifactClass";
  private static final String ARTIFACT_INIT_PARAMS = "initParams";
  private static final String ADD_OPERATION = "add";
  private static final String TDS_EQUAL_MESSAGE = "The Thing Descriptions should be equal";
  private static final String OPERATION_FAIL_MESSAGE =
      "The operation should have failed with 'Internal Server Error' status code";
  private static final String OPERATION_SUCCESS_MESSAGE =
      "The operation should have succeeded with an Ok status code";
  public static final String IRIS_EQUAL_MESSAGE = "The IRIs should be equal";
  public static final String PROPERTIES_EQUAL_MESSAGE = "The properties should be equal";

  private final BlockingQueue<RdfStoreMessage> storeMessageQueue;
  private final BlockingQueue<HttpNotificationDispatcherMessage> notificationQueue;
  private CartagoMessagebox cartagoMessagebox;
  private Optional<String> cartagoVerticleId;

  public CartagoVerticleTest() {
    this.storeMessageQueue = new LinkedBlockingQueue<>();
    this.notificationQueue = new LinkedBlockingQueue<>();
  }

  @BeforeEach
  public void setUp(final Vertx vertx) throws InterruptedException {
    this.cartagoMessagebox = new CartagoMessagebox(vertx.eventBus());
    final var storeMessagebox = new RdfStoreMessagebox(vertx.eventBus());
    storeMessagebox.init();
    storeMessagebox.receiveMessages(m -> this.storeMessageQueue.add(m.body()));
    final var notificationMessagebox =
        new HttpNotificationDispatcherMessagebox(vertx.eventBus());
    notificationMessagebox.init();
    notificationMessagebox.receiveMessages(m -> this.notificationQueue.add(m.body()));
    final var promise = Promise.<String>promise();
    vertx.deployVerticle(
        new CartagoVerticle(),
        new DeploymentOptions()
          .setConfig(new JsonObject(Map.of(
            "known-artifacts",
            Map.of(
              ADDER_SEMANTIC_TYPE,
              Adder.class.getCanonicalName(),
              COUNTER_SEMANTIC_TYPE,
              Counter.class.getCanonicalName()
            )
          ))),
        promise
    );
    final var latch = new CountDownLatch(1);
    promise.future()
           .onSuccess(i -> {
             this.cartagoVerticleId = Optional.of(i);
             latch.countDown();
           });
    latch.await();
  }

  @AfterEach
  public void tearDown(final Vertx vertx, final VertxTestContext ctx) {
    this.cartagoVerticleId.ifPresent(i -> vertx.undeploy(i, ctx.succeedingThenComplete()));
  }

  @Order(1)
  @Test
  public void createWorkspaceSucceeds(final VertxTestContext ctx)
      throws IOException, URISyntaxException {
    final var expectedThingDescription =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .onSuccess(r -> Assertions.assertEquals(
          expectedThingDescription,
          r.body(),
          TDS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Order(2)
  @Test
  public void createWorkspaceFailsWithAlreadyCreatedOne(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .onFailure(t -> Assertions.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode(),
          OPERATION_FAIL_MESSAGE
        ))
        .onComplete(ctx.failingThenComplete());
  }

  @Order(3)
  @Test
  public void joinWorkspaceSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var expectedBodyArtifactThingDescription =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("test_agent_body_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    final var expectedHypermediaBodyArtifactThingDescription = Files.readString(
        Path.of(ClassLoader.getSystemResource("test_agent_hypermedia_body_td.ttl").toURI()),
        StandardCharsets.UTF_8
    );
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.JoinWorkspace(AGENT_IRI, MAIN_WORKSPACE_NAME))
        .onSuccess(r -> {
          try {
            final var artifactsEntityCreationMessage =
                (RdfStoreMessage.CreateEntity) this.storeMessageQueue.take();
            Assertions.assertEquals(
                artifactsEntityCreationMessage.requestUri(),
                "http://localhost:8080/workspaces/" + MAIN_WORKSPACE_NAME + "/artifacts",
                "The URIs should be equal"
            );
            Assertions.assertEquals(
                artifactsEntityCreationMessage.entityName(),
                "body_" + AGENT_IRI,
                "The artifact names should be equal"
            );
            Assertions.assertEquals(
                artifactsEntityCreationMessage.entityRepresentation(),
                expectedBodyArtifactThingDescription,
                TDS_EQUAL_MESSAGE
            );
            final var hypermediaBodyCreationMessage =
                (RdfStoreMessage.CreateEntity) this.storeMessageQueue.take();
            Assertions.assertEquals(
                hypermediaBodyCreationMessage.requestUri(),
                "http://localhost:8080/workspaces/" + MAIN_WORKSPACE_NAME + "/artifacts/",
                "The URIs should be equal"
            );
            Assertions.assertEquals(
                hypermediaBodyCreationMessage.entityName(),
                "hypermedia_body_1",
                "The artifact names should be equal"
            );
            Assertions.assertEquals(
                hypermediaBodyCreationMessage.entityRepresentation(),
                expectedHypermediaBodyArtifactThingDescription,
                TDS_EQUAL_MESSAGE
            );
          } catch (final Exception e) {
            ctx.failNow(e);
          }
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  @Order(4)
  @Test
  public void joinWorkspaceIsIdempotent(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.JoinWorkspace(AGENT_IRI, MAIN_WORKSPACE_NAME))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Order(5)
  @Test
  public void joinWorkspaceFailsOnNonExistingOne(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.JoinWorkspace(AGENT_IRI, NONEXISTENT_NAME))
        .onFailure(t -> Assertions.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode(),
          OPERATION_FAIL_MESSAGE
        ))
        .onComplete(ctx.failingThenComplete());
  }

  @Order(6)
  @Test
  public void createSubWorkspaceSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var expectedWorkspaceThingDescription =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("sub_workspace_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateSubWorkspace(MAIN_WORKSPACE_NAME, SUB_WORKSPACE_NAME))
        .onSuccess(r -> Assertions.assertEquals(
            expectedWorkspaceThingDescription,
            r.body(),
            OPERATION_FAIL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Order(7)
  @Test
  public void createSubWorkspaceOfSubWorkspaceSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var expectedWorkspaceThingDescription =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("sub2_workspace_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateSubWorkspace(SUB_WORKSPACE_NAME, "sub2"))
        .onSuccess(r -> Assertions.assertEquals(
            expectedWorkspaceThingDescription,
            r.body(),
          TDS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Order(8)
  @Test
  public void createSubWorkspaceFailsOnNonExistingOne(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateSubWorkspace(NONEXISTENT_NAME, SUB_WORKSPACE_NAME))
        .onFailure(t -> Assertions.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode(),
          OPERATION_FAIL_MESSAGE
        ))
        .onComplete(ctx.failingThenComplete());
  }

  @Order(9)
  @Test
  public void createSubWorkspaceFailsOnAlreadyCreatedOne(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateSubWorkspace(MAIN_WORKSPACE_NAME, SUB_WORKSPACE_NAME))
        .onFailure(t -> Assertions.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode(),
          OPERATION_FAIL_MESSAGE
        ))
        .onComplete(ctx.failingThenComplete());
  }

  @Order(10)
  @Test
  public void leaveWorkspaceSucceeds(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.LeaveWorkspace(AGENT_IRI, MAIN_WORKSPACE_NAME))
        .onSuccess(r -> Assertions.assertEquals(
          String.valueOf(HttpStatus.SC_OK),
          r.body(),
          OPERATION_SUCCESS_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Order(11)
  @Test
  public void leaveWorkspaceFailsOnNotJoinedOne(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.LeaveWorkspace(AGENT_IRI, MAIN_WORKSPACE_NAME))
        .onFailure(t -> Assertions.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode(),
          OPERATION_FAIL_MESSAGE
        ))
        .onComplete(ctx.failingThenComplete());
  }

  @Order(12)
  @Test
  public void createArtifactWithoutParametersSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var expectedCounterArtifactThingDescription =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("c0_counter_artifact.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "c0",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            COUNTER_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        ))
        .onSuccess(r -> Assertions.assertEquals(
          expectedCounterArtifactThingDescription,
          r.body(),
          TDS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Order(13)
  @Test
  public void createArtifactWithParametersSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var expectedCounterArtifactThingDescription =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("c1_counter_artifact.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          AGENT_IRI,
          SUB_WORKSPACE_NAME,
          "c1",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            COUNTER_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of(5)
          ))
        ))
        .onSuccess(r -> Assertions.assertEquals(
            expectedCounterArtifactThingDescription,
            r.body(),
            TDS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Order(14)
  @Test
  public void createArtifactWithFeedbackParameterSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var expectedCounterArtifactThingDescription =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("a0_adder_artifact.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "a0",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            ADDER_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        ))
        .onSuccess(r -> Assertions.assertEquals(
            expectedCounterArtifactThingDescription,
            r.body(),
            TDS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Order(15)
  @Test
  public void createArtifactFailsWithUnknownClass(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          NONEXISTENT_NAME,
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            "http://www.example.org/NonExistentArtifact",
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        ))
        .onFailure(t -> Assertions.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode(),
          OPERATION_FAIL_MESSAGE
        ))
        .onComplete(ctx.failingThenComplete());
  }

  @Order(16)
  @Test
  public void createArtifactFailsWithUnknownWorkspace(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          AGENT_IRI,
          NONEXISTENT_NAME,
          "a1",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            ADDER_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        ))
        .onFailure(t -> Assertions.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode(),
          OPERATION_FAIL_MESSAGE
        ))
        .onComplete(ctx.failingThenComplete());
  }

  @Order(17)
  @Test
  public void createArtifactFailsWithWrongParameters(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "a1",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            ADDER_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of(2, 2)
          ))
        ))
        .onFailure(t -> Assertions.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode(),
          OPERATION_FAIL_MESSAGE
        ))
        .onComplete(ctx.failingThenComplete());
  }

  @Order(18)
  @Test
  public void focusSucceeds(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.Focus(
          FOCUSING_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "c0",
          CALLBACK_IRI
        ))
        .onSuccess(r -> {
          Assertions.assertEquals(
              String.valueOf(HttpStatus.SC_OK),
              r.body(),
              OPERATION_SUCCESS_MESSAGE
          );
          try {
            final var notifyPropertyMessage =
                (HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated)
                  this.notificationQueue.take();
            Assertions.assertEquals(
                notifyPropertyMessage.requestIri(),
                COUNTER_ARTIFACT_IRI,
                IRIS_EQUAL_MESSAGE
            );
            Assertions.assertEquals(
                notifyPropertyMessage.content(),
                "count(0)",
                PROPERTIES_EQUAL_MESSAGE
            );
          } catch (Exception e) {
            ctx.failNow(e);
          }
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  @Order(19)
  @Test
  public void focusFailsWithNonexistentWorkspace(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.Focus(
          FOCUSING_AGENT_IRI,
          NONEXISTENT_NAME,
          "c0",
          CALLBACK_IRI
        ))
        .onFailure(t -> Assertions.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode(),
          OPERATION_FAIL_MESSAGE
        ))
        .onComplete(ctx.failingThenComplete());
  }

  @Order(20)
  @Test
  public void focusFailsWithNonexistentArtifactName(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.Focus(
          FOCUSING_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          NONEXISTENT_NAME,
          CALLBACK_IRI
        ))
        .onFailure(t -> Assertions.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode(),
          OPERATION_FAIL_MESSAGE
        ))
        .onComplete(ctx.failingThenComplete());
  }

  @Order(21)
  @Test
  public void focusIsIdempotent(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.Focus(
          FOCUSING_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "c0",
          CALLBACK_IRI
        ))
        .onSuccess(r -> {
          Assertions.assertEquals(
              String.valueOf(HttpStatus.SC_OK),
              r.body(),
              OPERATION_SUCCESS_MESSAGE
          );
          try {
            final var notifyPropertyMessage =
                (HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated)
                  this.notificationQueue.take();
            Assertions.assertEquals(
                notifyPropertyMessage.requestIri(),
                COUNTER_ARTIFACT_IRI,
                IRIS_EQUAL_MESSAGE
            );
            Assertions.assertEquals(
                notifyPropertyMessage.content(),
                "count(0)",
                PROPERTIES_EQUAL_MESSAGE
            );
          } catch (Exception e) {
            ctx.failNow(e);
          }
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  @Order(22)
  @Test
  public void doActionSucceeds(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.DoAction(
          AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "c0",
          "inc",
          Optional.empty()
        ))
        .onSuccess(r -> Assertions.assertEquals(
            String.valueOf(HttpStatus.SC_OK),
            r.body(),
            OPERATION_SUCCESS_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Order(23)
  @Test
  public void doActionAfterFocusSucceeds(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.Focus(
          FOCUSING_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "c0",
          CALLBACK_IRI
        ))
        .compose(r -> {
          Assertions.assertEquals(
              String.valueOf(HttpStatus.SC_OK),
              r.body(),
              OPERATION_SUCCESS_MESSAGE
          );
          return this.cartagoMessagebox.sendMessage(new CartagoMessage.DoAction(
            AGENT_IRI,
            MAIN_WORKSPACE_NAME,
            "c0",
            "inc",
            Optional.empty()
          ));
        })
        .onSuccess(r -> {
          Assertions.assertEquals(
              String.valueOf(HttpStatus.SC_OK),
              r.body(),
              OPERATION_SUCCESS_MESSAGE
          );
          try {
            final var notifyPropertyMessage =
                (HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated)
                  this.notificationQueue.take();
            Assertions.assertEquals(
                COUNTER_ARTIFACT_IRI,
                notifyPropertyMessage.requestIri(),
                IRIS_EQUAL_MESSAGE
            );
            Assertions.assertEquals(
                "count(1)",
                notifyPropertyMessage.content(),
                PROPERTIES_EQUAL_MESSAGE
            );
          } catch (Exception e) {
            ctx.failNow(e);
          }
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  @Order(24)
  @Test
  public void doActionWithFeedbackParameterSucceeds(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.DoAction(
          AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "a0",
          ADD_OPERATION,
          Optional.of(CartagoDataBundle.toJson(List.of(2, 2)))
        ))
        .onSuccess(r -> Assertions.assertEquals(
            String.valueOf(4),
            r.body(),
            "The results should be equal"
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Order(25)
  @Test
  public void doActionFailsWithNonexistentWorkspace(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.DoAction(
          AGENT_IRI,
          NONEXISTENT_NAME,
          "a0",
          ADD_OPERATION,
          Optional.of(CartagoDataBundle.toJson(List.of(2, 2)))
        ))
        .onFailure(t -> Assertions.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode(),
          OPERATION_FAIL_MESSAGE
        ))
        .onComplete(ctx.failingThenComplete());
  }

  @Order(26)
  @Test
  public void doActionFailsWithNonexistentArtifact(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.DoAction(
          AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          NONEXISTENT_NAME,
          ADD_OPERATION,
          Optional.of(CartagoDataBundle.toJson(List.of(2, 2)))
        ))
        .onFailure(t -> Assertions.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode(),
          OPERATION_FAIL_MESSAGE
        ))
        .onComplete(ctx.failingThenComplete());
  }

  @Order(27)
  @Test
  public void doActionFailsWithNonexistentOperation(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.DoAction(
          AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "a0",
          NONEXISTENT_NAME,
          Optional.of(CartagoDataBundle.toJson(List.of(2, 2)))
        ))
        .onFailure(t -> Assertions.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode(),
          OPERATION_FAIL_MESSAGE
        ))
        .onComplete(ctx.failingThenComplete());
  }

  @Order(28)
  @Test
  public void doActionFailsWithWrongParameters(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.DoAction(
          AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "a0",
          ADD_OPERATION,
          Optional.of(CartagoDataBundle.toJson(List.of(2, "2", 5.0)))
        ))
        .onFailure(t -> Assertions.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode(),
          OPERATION_FAIL_MESSAGE
        ))
        .onComplete(ctx.failingThenComplete());
  }
}
