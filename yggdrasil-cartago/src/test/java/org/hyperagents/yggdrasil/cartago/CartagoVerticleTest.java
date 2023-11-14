package org.hyperagents.yggdrasil.cartago;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
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
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@RunWith(VertxUnitRunner.class)
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

  private final BlockingQueue<RdfStoreMessage> storeMessageQueue;
  private final BlockingQueue<HttpNotificationDispatcherMessage> notificationQueue;
  private Vertx vertx;
  private CartagoMessagebox cartagoMessagebox;
  private Optional<String> cartagoVerticleId;

  public CartagoVerticleTest() {
    this.storeMessageQueue = new LinkedBlockingQueue<>();
    this.notificationQueue = new LinkedBlockingQueue<>();
  }

  @Before
  public void setUp() throws InterruptedException {
    this.vertx = Vertx.vertx();
    this.cartagoMessagebox = new CartagoMessagebox(this.vertx.eventBus());
    final var storeMessagebox = new RdfStoreMessagebox(this.vertx.eventBus());
    storeMessagebox.init();
    storeMessagebox.receiveMessages(m -> this.storeMessageQueue.add(m.body()));
    final var notificationMessagebox =
        new HttpNotificationDispatcherMessagebox(this.vertx.eventBus());
    notificationMessagebox.init();
    notificationMessagebox.receiveMessages(m -> this.notificationQueue.add(m.body()));
    final var promise = Promise.<String>promise();
    this.vertx.deployVerticle(
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

  @After
  public void tearDown(final TestContext ctx) {
    this.cartagoVerticleId.ifPresent(i -> this.vertx.undeploy(i, ctx.asyncAssertSuccess()));
  }

  @Test
  public void test01CreateWorkspace(final TestContext ctx) throws IOException, URISyntaxException {
    final var expectedThingDescription =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .onSuccess(r -> ctx.assertEquals(expectedThingDescription, r.body()))
        .onComplete(ctx.asyncAssertSuccess());
  }

  @Test
  public void test02CreateWorkspaceFailsWithAlreadyCreatedOne(final TestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .onFailure(t -> ctx.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode()
        ))
        .onComplete(ctx.asyncAssertFailure());
  }

  @Test
  public void test03JoinWorkspace(final TestContext ctx) throws URISyntaxException, IOException {
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
            ctx.assertEquals(
                artifactsEntityCreationMessage.requestUri(),
                "http://localhost:8080/workspaces/" + MAIN_WORKSPACE_NAME + "/artifacts"
            );
            ctx.assertEquals(
                artifactsEntityCreationMessage.entityName(),
                "body_" + AGENT_IRI
            );
            ctx.assertEquals(
                artifactsEntityCreationMessage.entityRepresentation(),
                expectedBodyArtifactThingDescription
            );
            final var hypermediaBodyCreationMessage =
                (RdfStoreMessage.CreateEntity) this.storeMessageQueue.take();
            ctx.assertEquals(
                hypermediaBodyCreationMessage.requestUri(),
                "http://localhost:8080/workspaces/" + MAIN_WORKSPACE_NAME + "/artifacts/"
            );
            ctx.assertEquals(
                hypermediaBodyCreationMessage.entityName(),
                "hypermedia_body_1"
            );
            ctx.assertEquals(
                hypermediaBodyCreationMessage.entityRepresentation(),
                expectedHypermediaBodyArtifactThingDescription
            );
          } catch (final Exception e) {
            ctx.fail(e);
          }
        })
        .onComplete(ctx.asyncAssertSuccess());
  }

  @Test
  public void test04JoinWorkspaceIsIdempotent(final TestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.JoinWorkspace(AGENT_IRI, MAIN_WORKSPACE_NAME))
        .onComplete(ctx.asyncAssertSuccess());
  }

  @Test
  public void test05JoinWorkspaceFailsOnNonExistingOne(final TestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.JoinWorkspace(AGENT_IRI, NONEXISTENT_NAME))
        .onFailure(t -> ctx.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode()
        ))
        .onComplete(ctx.asyncAssertFailure());
  }

  @Test
  public void test06CreateSubWorkspace(final TestContext ctx)
      throws URISyntaxException, IOException {
    final var expectedWorkspaceThingDescription =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("sub_workspace_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateSubWorkspace(MAIN_WORKSPACE_NAME, SUB_WORKSPACE_NAME))
        .onSuccess(r -> ctx.assertEquals(expectedWorkspaceThingDescription, r.body()))
        .onComplete(ctx.asyncAssertSuccess());
  }

  @Test
  public void test07CreateSubWorkspaceOfSubWorkspace(final TestContext ctx)
      throws URISyntaxException, IOException {
    final var expectedWorkspaceThingDescription =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("sub2_workspace_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateSubWorkspace(SUB_WORKSPACE_NAME, "sub2"))
        .onSuccess(r -> ctx.assertEquals(expectedWorkspaceThingDescription, r.body()))
        .onComplete(ctx.asyncAssertSuccess());
  }

  @Test
  public void test08CreateSubWorkspaceFailsOnNonExistingOne(final TestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateSubWorkspace(NONEXISTENT_NAME, SUB_WORKSPACE_NAME))
        .onFailure(t -> ctx.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode()
        ))
        .onComplete(ctx.asyncAssertFailure());
  }

  @Test
  public void test09CreateSubWorkspaceFailsOnAlreadyCreatedOne(final TestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateSubWorkspace(MAIN_WORKSPACE_NAME, SUB_WORKSPACE_NAME))
        .onFailure(t -> ctx.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode()
        ))
        .onComplete(ctx.asyncAssertFailure());
  }

  @Test
  public void test10LeaveWorkspace(final TestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.LeaveWorkspace(AGENT_IRI, MAIN_WORKSPACE_NAME))
        .onSuccess(r -> ctx.assertEquals(String.valueOf(HttpStatus.SC_OK), r.body()))
        .onComplete(ctx.asyncAssertSuccess());
  }

  @Test
  public void test11LeaveWorkspaceFailsOnNotJoinedOne(final TestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.LeaveWorkspace(AGENT_IRI, MAIN_WORKSPACE_NAME))
        .onFailure(t -> ctx.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode()
        ))
        .onComplete(ctx.asyncAssertFailure());
  }

  @Test
  public void test12CreateArtifactWithoutParameters(final TestContext ctx)
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
        .onSuccess(r -> ctx.assertEquals(expectedCounterArtifactThingDescription, r.body()))
        .onComplete(ctx.asyncAssertSuccess());
  }

  @Test
  public void test13CreateArtifactWithParameters(final TestContext ctx)
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
        .onSuccess(r -> ctx.assertEquals(expectedCounterArtifactThingDescription, r.body()))
        .onComplete(ctx.asyncAssertSuccess());
  }

  @Test
  public void test14CreateArtifactWithFeedbackParameter(final TestContext ctx)
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
        .onSuccess(r -> ctx.assertEquals(expectedCounterArtifactThingDescription, r.body()))
        .onComplete(ctx.asyncAssertSuccess());
  }

  @Test
  public void test15CreateArtifactFailsWithUnknownClass(final TestContext ctx) {
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
        .onFailure(t -> ctx.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode()
        ))
        .onComplete(ctx.asyncAssertFailure());
  }

  @Test
  public void test16CreateArtifactFailsWithUnknownWorkspace(final TestContext ctx) {
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
        .onFailure(t -> ctx.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode()
        ))
        .onComplete(ctx.asyncAssertFailure());
  }

  @Test
  public void test17CreateArtifactFailsWithWrongParameters(final TestContext ctx) {
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
        .onFailure(t -> ctx.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode()
        ))
        .onComplete(ctx.asyncAssertFailure());
  }

  @Test
  public void test18Focus(final TestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.Focus(
          FOCUSING_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "c0",
          CALLBACK_IRI
        ))
        .onSuccess(r -> {
          ctx.assertEquals(String.valueOf(HttpStatus.SC_OK), r.body());
          try {
            final var notifyPropertyMessage =
                (HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated)
                  this.notificationQueue.take();
            ctx.assertEquals(notifyPropertyMessage.requestIri(), COUNTER_ARTIFACT_IRI);
            ctx.assertEquals(notifyPropertyMessage.content(), "count(0)");
          } catch (Exception e) {
            ctx.fail(e);
          }
        })
        .onComplete(ctx.asyncAssertSuccess());
  }

  @Test
  public void test19FocusFailsWithNonexistentWorkspace(final TestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.Focus(
          FOCUSING_AGENT_IRI,
          NONEXISTENT_NAME,
          "c0",
          CALLBACK_IRI
        ))
        .onFailure(t -> ctx.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode()
        ))
        .onComplete(ctx.asyncAssertFailure());
  }

  @Test
  public void test20FocusFailsWithNonexistentArtifactName(final TestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.Focus(
          FOCUSING_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          NONEXISTENT_NAME,
          CALLBACK_IRI
        ))
        .onFailure(t -> ctx.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode()
        ))
        .onComplete(ctx.asyncAssertFailure());
  }

  @Test
  public void test21FocusIsIdempotent(final TestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.Focus(
          FOCUSING_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "c0",
          CALLBACK_IRI
        ))
        .onSuccess(r -> {
          ctx.assertEquals(String.valueOf(HttpStatus.SC_OK), r.body());
          try {
            final var notifyPropertyMessage =
                (HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated)
                  this.notificationQueue.take();
            ctx.assertEquals(notifyPropertyMessage.requestIri(), COUNTER_ARTIFACT_IRI);
            ctx.assertEquals(notifyPropertyMessage.content(), "count(0)");
          } catch (Exception e) {
            ctx.fail(e);
          }
        })
        .onComplete(ctx.asyncAssertSuccess());
  }

  @Test
  public void test22DoAction(final TestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.DoAction(
          AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "c0",
          "inc",
          Optional.empty()
        ))
        .onSuccess(r -> ctx.assertEquals(String.valueOf(HttpStatus.SC_OK), r.body()))
        .onComplete(ctx.asyncAssertSuccess());
  }

  @Test
  public void test23DoActionAfterFocus(final TestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.Focus(
          FOCUSING_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "c0",
          CALLBACK_IRI
        ))
        .compose(r -> {
          ctx.assertEquals(String.valueOf(HttpStatus.SC_OK), r.body());
          return this.cartagoMessagebox.sendMessage(new CartagoMessage.DoAction(
            AGENT_IRI,
            MAIN_WORKSPACE_NAME,
            "c0",
            "inc",
            Optional.empty()
          ));
        })
        .onSuccess(r -> {
          ctx.assertEquals(String.valueOf(HttpStatus.SC_OK), r.body());
          try {
            final var notifyPropertyMessage =
                (HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated)
                  this.notificationQueue.take();
            ctx.assertEquals(COUNTER_ARTIFACT_IRI, notifyPropertyMessage.requestIri());
            ctx.assertEquals("count(1)", notifyPropertyMessage.content());
          } catch (Exception e) {
            ctx.fail(e);
          }
        })
        .onComplete(ctx.asyncAssertSuccess());
  }

  @Test
  public void test24DoActionWithFeedbackParameter(final TestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.DoAction(
          AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "a0",
          ADD_OPERATION,
          Optional.of(CartagoDataBundle.toJson(List.of(2, 2)))
        ))
        .onSuccess(r -> ctx.assertEquals(String.valueOf(4), r.body()))
        .onComplete(ctx.asyncAssertSuccess());
  }

  @Test
  public void test25DoActionFailsWithNonexistentWorkspace(final TestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.DoAction(
          AGENT_IRI,
          NONEXISTENT_NAME,
          "a0",
          ADD_OPERATION,
          Optional.of(CartagoDataBundle.toJson(List.of(2, 2)))
        ))
        .onFailure(t -> ctx.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode()
        ))
        .onComplete(ctx.asyncAssertFailure());
  }

  @Test
  public void test26DoActionFailsWithNonexistentArtifact(final TestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.DoAction(
          AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          NONEXISTENT_NAME,
          ADD_OPERATION,
          Optional.of(CartagoDataBundle.toJson(List.of(2, 2)))
        ))
        .onFailure(t -> ctx.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode()
        ))
        .onComplete(ctx.asyncAssertFailure());
  }

  @Test
  public void test27DoActionFailsWithNonexistentOperation(final TestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.DoAction(
          AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "a0",
          NONEXISTENT_NAME,
          Optional.of(CartagoDataBundle.toJson(List.of(2, 2)))
        ))
        .onFailure(t -> ctx.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode()
        ))
        .onComplete(ctx.asyncAssertFailure());
  }

  @Test
  public void test28DoActionFailsWithWrongParameters(final TestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.DoAction(
          AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "a0",
          ADD_OPERATION,
          Optional.of(CartagoDataBundle.toJson(List.of(2, "2", 5.0)))
        ))
        .onFailure(t -> ctx.assertEquals(
          HttpStatus.SC_INTERNAL_SERVER_ERROR,
          ((ReplyException) t).failureCode()
        ))
        .onComplete(ctx.asyncAssertFailure());
  }
}
