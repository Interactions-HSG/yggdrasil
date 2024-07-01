package org.hyperagents.yggdrasil.cartago;

import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphReader;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
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
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.hc.core5.http.HttpStatus;
import org.eclipse.rdf4j.model.util.Models;
import org.hyperagents.yggdrasil.cartago.artifacts.*;
import org.hyperagents.yggdrasil.eventbus.messageboxes.CartagoMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.model.Environment;
import org.hyperagents.yggdrasil.model.impl.EnvironmentParser;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.impl.EnvironmentConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.WebSubConfigImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
public class CartagoVerticleHMASTest {
  private static final String MAIN_WORKSPACE_NAME = "test";
  private static final String SUB_WORKSPACE_NAME = "sub";
  private static final String CLASS = "class";
  private static final String TEMPLATE = "template";
  private static final String TEST_AGENT_IRI = "http://localhost:8080/agents/test";
  private static final String FOCUSING_AGENT_IRI = "http://localhost:8080/agents/focusing_agent";
  private static final String TEST_AGENT_BODY_URI =
    "http://localhost:8080/workspaces/" + SUB_WORKSPACE_NAME + "/artifacts/test/";
  private static final String ADDER_SEMANTIC_TYPE = "http://example.org/Adder";
  private static final String COUNTER_SEMANTIC_TYPE = "http://example.org/Counter";
  private static final String SIGNAL_SEMANTIC_TYPE = "http://example.org/Sign";
  private static final String MATH_SEMANTIC_TYPE = "http://example.org/Math";
  private static final String NONEXISTENT_NAME = "nonexistent";
  private static final String ARTIFACT_SEMANTIC_TYPE_PARAM = "artifactClass";
  private static final String ARTIFACT_INIT_PARAMS = "initParams";
  private static final String INCREMENT_OPERATION = "inc";
  private static final String ADD_OPERATION = "add";
  private static final String TDS_EQUAL_MESSAGE = "The Thing Descriptions should be equal";
  private static final String OPERATION_FAIL_MESSAGE =
    "The operation should have failed with 'Internal Server Error' status code";
  private static final String OPERATION_SUCCESS_MESSAGE =
    "The operation should have succeeded with an Ok status code";
  private static final String URIS_EQUAL_MESSAGE = "The URIs should be equal";
  private static final String DEFAULT_CONFIG_VALUE = "default";

  private final BlockingQueue<HttpNotificationDispatcherMessage> notificationQueue;
  private CartagoMessagebox cartagoMessagebox;

  public CartagoVerticleHMASTest() {
    this.notificationQueue = new LinkedBlockingQueue<>();
  }

  private static String getArtifactsIriFromWorkspace(final String workspace) {
    return "http://localhost:8080/workspaces/" + workspace + "/artifacts/";
  }

  @BeforeEach
  public void setUp(final Vertx vertx, final VertxTestContext ctx) {
    final var httpConfig = new HttpInterfaceConfigImpl(JsonObject.of());
    vertx.sharedData()
      .<String, HttpInterfaceConfig>getLocalMap("http-config")
      .put(DEFAULT_CONFIG_VALUE, httpConfig);
    final var environmentConfig =
      new EnvironmentConfigImpl(JsonObject.of(
        "environment-config",
        JsonObject.of("enabled", true,
          "ontology",
          "hmas")
      ));
    vertx.sharedData()
      .<String, EnvironmentConfig>getLocalMap("environment-config")
      .put(DEFAULT_CONFIG_VALUE, environmentConfig);
    vertx.sharedData()
      .<String, Environment>getLocalMap("environment")
      .put(DEFAULT_CONFIG_VALUE, EnvironmentParser.parse(JsonObject.of(
        "environment-config",
        JsonObject.of(
          "known-artifacts",
          JsonArray.of(
            JsonObject.of(
              CLASS,
              ADDER_SEMANTIC_TYPE,
              TEMPLATE,
              AdderHMAS.class.getCanonicalName()
            ),
            JsonObject.of(
              CLASS,
              COUNTER_SEMANTIC_TYPE,
              TEMPLATE,
              CounterHMAS.class.getCanonicalName()
            ),
            JsonObject.of(
              CLASS,
              SIGNAL_SEMANTIC_TYPE,
              TEMPLATE,
              SignalerHMAS.class.getCanonicalName()
            ),
            JsonObject.of(
              CLASS,
              MATH_SEMANTIC_TYPE,
              TEMPLATE,
              MathHMAS.class.getCanonicalName()
            )
          )
        )

      )));
    final var notificationConfig = new WebSubConfigImpl(
      JsonObject.of(
        "notification-config",
        JsonObject.of("enabled", true)
      ),
      httpConfig
    );
    vertx.sharedData()
      .getLocalMap("notification-config")
      .put(DEFAULT_CONFIG_VALUE, notificationConfig);
    this.cartagoMessagebox = new CartagoMessagebox(
      vertx.eventBus(),
      environmentConfig
    );
    final var notificationMessagebox = new HttpNotificationDispatcherMessagebox(
      vertx.eventBus(),
      notificationConfig
    );
    notificationMessagebox.init();
    notificationMessagebox.receiveMessages(m -> this.notificationQueue.add(m.body()));
    vertx.deployVerticle(new CartagoVerticle(), ctx.succeedingThenComplete());
  }

  @AfterEach
  public void tearDown(final Vertx vertx, final VertxTestContext ctx) {
    vertx.close(ctx.succeedingThenComplete());
  }

  @Test
  public void testCreateWorkspaceSucceedsHMAS(final VertxTestContext ctx)
    throws IOException, URISyntaxException {
    final var expectedThingDescription =
      Files.readString(
        Path.of(ClassLoader.getSystemResource("hmas/test_workspace_hmas.ttl").toURI()),
        StandardCharsets.UTF_8
      );
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .onSuccess(r -> assertEqualsHMASDescriptions(
        expectedThingDescription,
        r.body()
      ))
      .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testCreateWorkspaceFailsWithAlreadyCreatedOneHMAS(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME)))
      .onFailure(t -> Assertions.assertEquals(
        HttpStatus.SC_INTERNAL_SERVER_ERROR,
        ((ReplyException) t).failureCode(),
        OPERATION_FAIL_MESSAGE
      ))
      .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testJoinWorkspaceSucceedsHMAS(final VertxTestContext ctx)
    throws URISyntaxException, IOException {
    final var expectedBodyThingDescription =
      Files.readString(
        Path.of(ClassLoader.getSystemResource("hmas/test_agent_body_hmas.ttl").toURI()),
        StandardCharsets.UTF_8
      );
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.JoinWorkspace(
          TEST_AGENT_IRI,
          null,
          MAIN_WORKSPACE_NAME
        )))
      .onSuccess(r -> assertEqualsHMASDescriptions(
        expectedBodyThingDescription,
        r.body()
      ))
      .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testJoinWorkspaceIsIdempotentHMAS(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.JoinWorkspace(
          TEST_AGENT_IRI,
          null,
          MAIN_WORKSPACE_NAME
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.JoinWorkspace(
          TEST_AGENT_IRI,
          null,
          MAIN_WORKSPACE_NAME
        )))
      .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testJoinWorkspaceFailsOnNonExistingOneHMAS(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.JoinWorkspace(TEST_AGENT_IRI, null, NONEXISTENT_NAME))
      .onFailure(t -> Assertions.assertEquals(
        HttpStatus.SC_INTERNAL_SERVER_ERROR,
        ((ReplyException) t).failureCode(),
        OPERATION_FAIL_MESSAGE
      ))
      .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testCreateSubWorkspaceSucceedsHMAS(final VertxTestContext ctx)
    throws URISyntaxException, IOException {
    final var expectedWorkspaceThingDescription =
      Files.readString(
        Path.of(ClassLoader.getSystemResource("hmas/sub_workspace_hmas.ttl").toURI()),
        StandardCharsets.UTF_8
      );
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateSubWorkspace(
          MAIN_WORKSPACE_NAME,
          SUB_WORKSPACE_NAME
        )))
      .onSuccess(r -> assertEqualsHMASDescriptions(
        expectedWorkspaceThingDescription,
        r.body()
      ))
      .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testCreateSubWorkspaceOfSubWorkspaceSucceedsHMAS(final VertxTestContext ctx)
    throws URISyntaxException, IOException {
    final var expectedWorkspaceThingDescription =
      Files.readString(
        Path.of(ClassLoader.getSystemResource("hmas/sub2_workspace_hmas.ttl").toURI()),
        StandardCharsets.UTF_8
      );
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateSubWorkspace(
          MAIN_WORKSPACE_NAME,
          SUB_WORKSPACE_NAME
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateSubWorkspace(
          SUB_WORKSPACE_NAME,
          "sub2"
        )))
      .onSuccess(r -> assertEqualsHMASDescriptions(
        expectedWorkspaceThingDescription,
        r.body()
      ))
      .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testCreateSubWorkspaceFailsOnNonExistingOneHMAS(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateSubWorkspace(NONEXISTENT_NAME, SUB_WORKSPACE_NAME))
      .onFailure(t -> Assertions.assertEquals(
        HttpStatus.SC_INTERNAL_SERVER_ERROR,
        ((ReplyException) t).failureCode(),
        OPERATION_FAIL_MESSAGE
      ))
      .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testCreateSubWorkspaceFailsOnAlreadyCreatedOneHMAS(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateSubWorkspace(
          MAIN_WORKSPACE_NAME,
          SUB_WORKSPACE_NAME
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateSubWorkspace(
          MAIN_WORKSPACE_NAME,
          SUB_WORKSPACE_NAME
        )))
      .onFailure(t -> Assertions.assertEquals(
        HttpStatus.SC_INTERNAL_SERVER_ERROR,
        ((ReplyException) t).failureCode(),
        OPERATION_FAIL_MESSAGE
      ))
      .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testLeaveWorkspaceSucceedsHMAS(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.JoinWorkspace(
          TEST_AGENT_IRI,
          null,
          MAIN_WORKSPACE_NAME
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.LeaveWorkspace(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME
        )))
      .onSuccess(r -> Assertions.assertEquals(
        String.valueOf(HttpStatus.SC_OK),
        r.body(),
        OPERATION_SUCCESS_MESSAGE
      ))
      .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testLeaveWorkspaceFailsOnNotJoinedOneHMAS(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.LeaveWorkspace(TEST_AGENT_IRI, MAIN_WORKSPACE_NAME))
      .onFailure(t -> Assertions.assertEquals(
        HttpStatus.SC_INTERNAL_SERVER_ERROR,
        ((ReplyException) t).failureCode(),
        OPERATION_FAIL_MESSAGE
      ))
      .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testCreateArtifactWithoutParametersSucceedsHMAS(final VertxTestContext ctx)
    throws URISyntaxException, IOException {
    final var expectedCounterArtifactThingDescription =
      Files.readString(
        Path.of(ClassLoader.getSystemResource("hmas/c0_counter_artifact_hmas.ttl").toURI()),
        StandardCharsets.UTF_8
      );
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "c0",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            COUNTER_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        )))
      .onSuccess(r -> assertEqualsHMASDescriptions(
        expectedCounterArtifactThingDescription,
        r.body()
      ))
      .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testCreateArtifactWithParametersSucceedsHMAS(final VertxTestContext ctx)
    throws URISyntaxException, IOException {
    final var expectedCounterArtifactThingDescription =
      Files.readString(
        Path.of(ClassLoader.getSystemResource("hmas/c1_counter_artifact_hmas.ttl").toURI()),
        StandardCharsets.UTF_8
      );
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateSubWorkspace(
          MAIN_WORKSPACE_NAME,
          SUB_WORKSPACE_NAME
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          TEST_AGENT_IRI,
          SUB_WORKSPACE_NAME,
          "c1",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            COUNTER_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of(5)
          ))
        )))
      .onSuccess(r -> assertEqualsHMASDescriptions(
        expectedCounterArtifactThingDescription,
        r.body()
      ))
      .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testCreateArtifactWithFeedbackParameterSucceedsHMAS(final VertxTestContext ctx)
    throws URISyntaxException, IOException {
    final var expectedAdderArtifactThingDescription =
      Files.readString(
        Path.of(ClassLoader.getSystemResource("hmas/a0_adder_artifact_hmas.ttl").toURI()),
        StandardCharsets.UTF_8
      );
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "a0",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            ADDER_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        )))
      .onSuccess(r -> assertEqualsHMASDescriptions(
        expectedAdderArtifactThingDescription,
        r.body()
      ))
      .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testCreateArtifactFailsWithUnknownClassHMAS(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          NONEXISTENT_NAME,
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            "http://www.example.org/NonExistentArtifact",
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        )))
      .onFailure(t -> Assertions.assertEquals(
        HttpStatus.SC_INTERNAL_SERVER_ERROR,
        ((ReplyException) t).failureCode(),
        OPERATION_FAIL_MESSAGE
      ))
      .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testCreateArtifactFailsWithUnknownWorkspaceHMAS(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          TEST_AGENT_IRI,
          NONEXISTENT_NAME,
          "a1",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            ADDER_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        )))
      .onFailure(t -> Assertions.assertEquals(
        HttpStatus.SC_INTERNAL_SERVER_ERROR,
        ((ReplyException) t).failureCode(),
        OPERATION_FAIL_MESSAGE
      ))
      .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testCreateArtifactFailsWithWrongParametersHMAS(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "a1",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            ADDER_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of(2, 2)
          ))
        )))
      .onFailure(t -> Assertions.assertEquals(
        HttpStatus.SC_INTERNAL_SERVER_ERROR,
        ((ReplyException) t).failureCode(),
        OPERATION_FAIL_MESSAGE
      ))
      .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testFocusSucceedsHMAS(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "c0",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            COUNTER_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.Focus(
          FOCUSING_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "c0"
        )))
      .onSuccess(r -> {
        Assertions.assertEquals(
          String.valueOf(HttpStatus.SC_OK),
          r.body(),
          OPERATION_SUCCESS_MESSAGE
        );
        try {
          assertNotificationReceived(
            MAIN_WORKSPACE_NAME,
            "c0",
            "count(0)"
          );
        } catch (Exception e) {
          ctx.failNow(e);
        }
      })
      .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testFocusFailsWithNonexistentWorkspaceHMAS(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "c0",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            COUNTER_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.Focus(
          FOCUSING_AGENT_IRI,
          NONEXISTENT_NAME,
          "c0"
        )))
      .onFailure(t -> Assertions.assertEquals(
        HttpStatus.SC_INTERNAL_SERVER_ERROR,
        ((ReplyException) t).failureCode(),
        OPERATION_FAIL_MESSAGE
      ))
      .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testFocusFailsWithNonexistentArtifactNameHMAS(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "c0",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            COUNTER_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.Focus(
          FOCUSING_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          NONEXISTENT_NAME
        )))
      .onFailure(t -> Assertions.assertEquals(
        HttpStatus.SC_INTERNAL_SERVER_ERROR,
        ((ReplyException) t).failureCode(),
        OPERATION_FAIL_MESSAGE
      ))
      .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testFocusIsIdempotentHMAS(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "c0",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            COUNTER_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.Focus(
          FOCUSING_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "c0"
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.Focus(
          FOCUSING_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "c0"
        )))
      .onSuccess(r -> {
        Assertions.assertEquals(
          String.valueOf(HttpStatus.SC_OK),
          r.body(),
          OPERATION_SUCCESS_MESSAGE
        );
        try {
          assertNotificationReceived(
            MAIN_WORKSPACE_NAME,
            "c0",
            "count(0)"
          );
        } catch (Exception e) {
          ctx.failNow(e);
        }
      })
      .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testDoActionSucceedsHMAS(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "c0",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            COUNTER_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.DoAction(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "c0",
          "POSThttp://localhost:8080/workspaces/test/artifacts/c0/increment",
          r.body(),
          ctx.toString()
        )))
      .onSuccess(r -> Assertions.assertNull(r.body(), OPERATION_SUCCESS_MESSAGE))
      .onComplete(ctx.succeedingThenComplete());
  }

  // TODO
  @Test
  public void testDoActionSendsSignalSucceedsHMAS(final VertxTestContext ctx) throws URISyntaxException, IOException {
    final var SIGNAL_ARTIFACT_HMAS =
      Files.readString(
        Path.of(ClassLoader.getSystemResource("hmas/signal_artifact_hmas.ttl").toURI()),
        StandardCharsets.UTF_8
      );
    final var TestRequests = this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateSubWorkspace(
          MAIN_WORKSPACE_NAME,
          SUB_WORKSPACE_NAME
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          TEST_AGENT_IRI,
          SUB_WORKSPACE_NAME,
          "c1",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            SIGNAL_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.Focus(
          FOCUSING_AGENT_IRI,
          SUB_WORKSPACE_NAME,
          "c1"
        )))
      .compose(r ->
        this.cartagoMessagebox
          .sendMessage(new CartagoMessage.DoAction(
            TEST_AGENT_IRI,
            SUB_WORKSPACE_NAME,
            "c1",
            "POSThttp://localhost:8080/workspaces/sub/artifacts/c1/sign",
            SIGNAL_ARTIFACT_HMAS,
            ""
          )));


    TestRequests.onComplete(r -> {
      try {
        final var actionRequested = (HttpNotificationDispatcherMessage.ActionRequested) this.notificationQueue.take();
        Assertions.assertEquals(
          actionRequested.requestIri(),
          "http://localhost:8080/workspaces/sub/artifacts/test/",
          URIS_EQUAL_MESSAGE
        );
        final var artifactObsPropertyUpdated = (HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated) this.notificationQueue.take();
        Assertions.assertEquals(
          artifactObsPropertyUpdated.content(),
          "tick",
          "Content should be equal"
        );
        final var actionSucceeded = (HttpNotificationDispatcherMessage.ActionSucceeded) this.notificationQueue.take();
        Assertions.assertEquals(
          actionSucceeded.requestIri(),
          "http://localhost:8080/workspaces/sub/artifacts/test/",
          URIS_EQUAL_MESSAGE
        );
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }


    }).andThen(ctx.succeedingThenComplete());


  }

  @Test
  public void testDoActionAfterFocusSucceedsHMAS(final VertxTestContext ctx) throws URISyntaxException, IOException {
    final var COUNTER_ARTIFACT_HMAS =
      Files.readString(
        Path.of(ClassLoader.getSystemResource("hmas/counter_artifact_hmas.ttl").toURI()),
        StandardCharsets.UTF_8
      );

    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateSubWorkspace(
          MAIN_WORKSPACE_NAME,
          SUB_WORKSPACE_NAME
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          TEST_AGENT_IRI,
          SUB_WORKSPACE_NAME,
          "c1",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            COUNTER_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of(5)
          ))
        )))
      .compose(r ->
        this.cartagoMessagebox
        .sendMessage(new CartagoMessage.Focus(
          FOCUSING_AGENT_IRI,
          SUB_WORKSPACE_NAME,
          "c1"
        )))
      .compose(r -> {
        Assertions.assertEquals(
          String.valueOf(HttpStatus.SC_OK),
          r.body(),
          OPERATION_SUCCESS_MESSAGE
        );
        try {
          assertNotificationReceived(
            SUB_WORKSPACE_NAME,
            "c1",
            "count(5)"
          );
        } catch (final Exception e) {
          ctx.failNow(e);
        }
        return this.cartagoMessagebox.sendMessage(new CartagoMessage.DoAction(
          TEST_AGENT_IRI,
          SUB_WORKSPACE_NAME,
          "c1",
          "POSThttp://localhost:8080/workspaces/test/artifacts/c0/increment",
          COUNTER_ARTIFACT_HMAS,
          ""
        ));
      })
      .onSuccess(r -> {
        Assertions.assertNull(r.body(), OPERATION_SUCCESS_MESSAGE);
        try {
          final var notifyActionRequestedMessage =
            (HttpNotificationDispatcherMessage.ActionRequested)
              this.notificationQueue.take();
          Assertions.assertEquals(
            TEST_AGENT_BODY_URI,
            notifyActionRequestedMessage.requestIri(),
            URIS_EQUAL_MESSAGE
          );
          Assertions.assertEquals(
            notifyActionRequestedMessage.content(),
            JsonObject
              .of(
                "artifactName",
                "c1",
                "actionName",
                INCREMENT_OPERATION
              )
              .encode(),
            "The properties should be equal"
          );
          assertNotificationReceived(
            SUB_WORKSPACE_NAME,
            "c1",
            "count(6)"
          );
          final var notifyActionCompletedMessage =
            (HttpNotificationDispatcherMessage.ActionSucceeded)
              this.notificationQueue.take();
          Assertions.assertEquals(
            TEST_AGENT_BODY_URI,
            notifyActionCompletedMessage.requestIri(),
            URIS_EQUAL_MESSAGE
          );
          Assertions.assertEquals(
            notifyActionCompletedMessage.content(),
            JsonObject
              .of(
                "artifactName",
                "c1",
                "actionName",
                INCREMENT_OPERATION
              )
              .encode(),
            "The properties should be equal"
          );
        } catch (final Exception e) {
          ctx.failNow(e);
        }
      })
      .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testDoActionSucceedsWithNoPayloadButOneFeedBackParams(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "m0",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            MATH_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.DoAction(
            TEST_AGENT_IRI,
            MAIN_WORKSPACE_NAME,
            "m0",
            "POSThttp://localhost:8080/workspaces/test/artifacts/m0/rand",
            r.body(),
            ""
          )
        ))
      .onSuccess(r -> Assertions.assertNotNull(
        r.body(),
        "Should be random integer"
      ))
      .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testDoActionSucceedsWithNoPayloadButTwoFeedBackParams(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "m0",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            MATH_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.DoAction(
            TEST_AGENT_IRI,
            MAIN_WORKSPACE_NAME,
            "m0",
            "POSThttp://localhost:8080/workspaces/test/artifacts/m0/rand2",
            r.body(),
            ""
          )
        ))
      .onSuccess(r -> Assertions.assertNotNull(
        r.body(),
        "Should be two random integer"
      ))
      .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testDoActionWithMultipleFeedbackParameterSucceeds(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "m0",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            MATH_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.DoAction(
            TEST_AGENT_IRI,
            MAIN_WORKSPACE_NAME,
            "m0",
            "POSThttp://localhost:8080/workspaces/test/artifacts/m0/egcd",
            r.body(),
            "[18,6]"
          )
        ))
      .onSuccess(r -> Assertions.assertEquals(
        "6, 0, 1",
        r.body(),
        "The results should be equal"
      ))
      .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testDoActionWithFeedbackParameterSucceedsHMAS(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "a0",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            ADDER_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.DoAction(
            TEST_AGENT_IRI,
            MAIN_WORKSPACE_NAME,
            "a0",
          "POSThttp://localhost:8080/workspaces/test/artifacts/a0/add",
            r.body(),
            "[2,2]"
          )
        ))
      .onSuccess(r -> Assertions.assertEquals(
        String.valueOf(4),
        r.body(),
        "The results should be equal"
      ))
      .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testDoActionFailsWithNonexistentWorkspaceHMAS(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "a0",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            ADDER_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.DoAction(
          TEST_AGENT_IRI,
          NONEXISTENT_NAME,
          "a0",
          ADD_OPERATION,
          r.body(),
          Optional.of(CartagoDataBundle.toJson(List.of(2, 2))).toString()
        )))
      .onFailure(t -> Assertions.assertEquals(
        HttpStatus.SC_INTERNAL_SERVER_ERROR,
        ((ReplyException) t).failureCode(),
        OPERATION_FAIL_MESSAGE
      ))
      .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testDoActionFailsWithNonexistentArtifactHMAS(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "a0",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            ADDER_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.DoAction(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          NONEXISTENT_NAME,
          ADD_OPERATION,
          r.body(),
          Optional.of(CartagoDataBundle.toJson(List.of(2, 2))).toString()
        )))
      .onFailure(t -> Assertions.assertEquals(
        HttpStatus.SC_INTERNAL_SERVER_ERROR,
        ((ReplyException) t).failureCode(),
        OPERATION_FAIL_MESSAGE
      ))
      .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testDoActionFailsWithNonexistentOperationHMAS(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "a0",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            ADDER_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.DoAction(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "a0",
          NONEXISTENT_NAME,
          r.body(),
          Optional.of(CartagoDataBundle.toJson(List.of(2, 2))).toString()
        )))
      .onFailure(t -> Assertions.assertEquals(
        HttpStatus.SC_INTERNAL_SERVER_ERROR,
        ((ReplyException) t).failureCode(),
        OPERATION_FAIL_MESSAGE
      ))
      .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testDoActionFailsWithWrongParametersHMAS(final VertxTestContext ctx) {
    this.cartagoMessagebox
      .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateArtifact(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "a0",
          Json.encode(Map.of(
            ARTIFACT_SEMANTIC_TYPE_PARAM,
            ADDER_SEMANTIC_TYPE,
            ARTIFACT_INIT_PARAMS,
            List.of()
          ))
        )))
      .compose(r -> this.cartagoMessagebox
        .sendMessage(new CartagoMessage.DoAction(
          TEST_AGENT_IRI,
          MAIN_WORKSPACE_NAME,
          "a0",
          ADD_OPERATION,
          r.body(),
          "[2,3,5,6]"
        )))
      .onFailure(t -> Assertions.assertEquals(
        HttpStatus.SC_INTERNAL_SERVER_ERROR,
        ((ReplyException) t).failureCode(),
        OPERATION_FAIL_MESSAGE
      ))
      .onComplete(ctx.failingThenComplete());
  }

  private void assertNotificationReceived(
    final String workspace,
    final String artifact,
    final String content
  ) throws InterruptedException {
    final var notifyPropertyMessage =
      (HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated)
        this.notificationQueue.take();
    Assertions.assertEquals(
      getArtifactsIriFromWorkspace(workspace) + artifact + "/",
      notifyPropertyMessage.requestIri(),
      URIS_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
      notifyPropertyMessage.content(),
      content,
      "The properties should be equal"
    );
  }

  private void assertEqualsHMASDescriptions(final String expected, final String actual) {
    final var areEqual = Models.isomorphic(
      ResourceProfileGraphReader.getModelFromString(expected),
      ResourceProfileGraphReader.getModelFromString(actual)
    );

    if(!areEqual) {
      System.out.println(actual);
    }
    Assertions.assertTrue(
      areEqual,
      TDS_EQUAL_MESSAGE
    );
  }
}
