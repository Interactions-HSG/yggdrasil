package org.hyperagents.yggdrasil.cartago;

import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
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
import org.hyperagents.yggdrasil.cartago.artifacts.AdderTD;
import org.hyperagents.yggdrasil.cartago.artifacts.CounterTD;
import org.hyperagents.yggdrasil.cartago.artifacts.MathTD;
import org.hyperagents.yggdrasil.eventbus.messageboxes.CartagoMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.model.interfaces.Environment;
import org.hyperagents.yggdrasil.model.parser.EnvironmentParser;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.impl.EnvironmentConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.WebSubConfigImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * class to test cartago with td ontology.
 */
@ExtendWith(VertxExtension.class)
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
public class CartagoVerticleTDTest {
  private static final String TEST = "test";
  private static final String MAIN_WORKSPACE_NAME = TEST;
  private static final String SUB_WORKSPACE_NAME = "sub";
  private static final String TEST_AGENT_IRI = "http://localhost:8080/agents/test";
  private static final String TEST_AGENT_BODY = "test_agent";
  private static final String FOCUSING_AGENT_IRI = "http://localhost:8080/agents/focusing_agent";
  private static final String TEST_AGENT_BODY_URI =
      "http://localhost:8080/workspaces/" + SUB_WORKSPACE_NAME + "/artifacts/body_test_agent";
  private static final String ADDER_SEMANTIC_TYPE = "http://example.org/Adder";
  private static final String MATH_SEMANTIC_TYPE = "http://example.org/Math";
  private static final String COUNTER_SEMANTIC_TYPE = "http://example.org/Counter";
  private static final String NONEXISTENT_NAME = "nonexistent";
  private static final String ARTIFACT_SEMANTIC_TYPE_PARAM = "artifactClass";
  private static final String ARTIFACT_INIT_PARAMS = "initParams";
  private static final String INCREMENT_OPERATION = "inc";
  private static final String ADD_OPERATION = "add";
  private static final String TDS_EQUAL_MESSAGE = "The Thing Descriptions should be equal";
  private static final String OPERATION_FAIL_MESSAGE =
      "The operation should have failed with 'Internal Server Error' status code";
  private static final String OPERATION_SUCCESS_MESSAGE =
      "The operation should have succeeded returning null if no feedback params";
  private static final String URIS_EQUAL_MESSAGE = "The URIs should be equal";
  private static final String DEFAULT_CONFIG_VALUE = "default";

  private final BlockingQueue<HttpNotificationDispatcherMessage> notificationQueue;
  private CartagoMessagebox cartagoMessagebox;

  public CartagoVerticleTDTest() {
    this.notificationQueue = new LinkedBlockingQueue<>();
  }

  private static String getArtifactsIriFromWorkspace(final String workspace) {
    return "http://localhost:8080/workspaces/" + workspace + "/artifacts/";
  }

  /**
   * setup method.
   *
   * @param vertx vertx
   * @param ctx ctx
   */
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
                "td")
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
                        "class",
                        ADDER_SEMANTIC_TYPE,
                        "template",
                        AdderTD.class.getCanonicalName()
                    ),
                    JsonObject.of(
                        "class",
                        COUNTER_SEMANTIC_TYPE,
                        "template",
                        CounterTD.class.getCanonicalName()
                    ),
                    JsonObject.of(
                        "class",
                        MATH_SEMANTIC_TYPE,
                        "template",
                        MathTD.class.getCanonicalName()
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
  public void testCreateWorkspaceSucceeds(final VertxTestContext ctx)
      throws IOException, URISyntaxException {
    final var expectedThingDescription =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("td/test_workspace_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .onSuccess(r -> assertEqualsThingDescriptions(expectedThingDescription, r.body()))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testCreateWorkspaceFailsWithAlreadyCreatedOne(final VertxTestContext ctx) {
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
  public void testJoinWorkspaceSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var expectedBodyThingDescription =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("td/test_agent_body_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox
            .sendMessage(new CartagoMessage.JoinWorkspace(
                TEST_AGENT_IRI,
                TEST_AGENT_BODY,
                MAIN_WORKSPACE_NAME
            )))
        .onSuccess(r -> assertEqualsThingDescriptions(
            expectedBodyThingDescription,
            r.body()
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testJoinWorkspaceIsIdempotent(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox
            .sendMessage(new CartagoMessage.JoinWorkspace(
                TEST_AGENT_IRI,
                TEST,
                MAIN_WORKSPACE_NAME
            )))
        .compose(r -> this.cartagoMessagebox
            .sendMessage(new CartagoMessage.JoinWorkspace(
                TEST_AGENT_IRI,
                TEST,
                MAIN_WORKSPACE_NAME
            )))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testJoinWorkspaceFailsOnNonExistingOne(final VertxTestContext ctx) {
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
  public void testCreateSubWorkspaceSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var expectedWorkspaceThingDescription =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("td/sub_workspace_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox
            .sendMessage(new CartagoMessage.CreateSubWorkspace(
                MAIN_WORKSPACE_NAME,
                SUB_WORKSPACE_NAME
            )))
        .onSuccess(r -> assertEqualsThingDescriptions(expectedWorkspaceThingDescription, r.body()))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testCreateSubWorkspaceOfSubWorkspaceSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var expectedWorkspaceThingDescription =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("td/sub2_workspace_td.ttl").toURI()),
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
        .onSuccess(r -> assertEqualsThingDescriptions(expectedWorkspaceThingDescription, r.body()))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testCreateSubWorkspaceFailsOnNonExistingOne(final VertxTestContext ctx) {
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
  public void testCreateSubWorkspaceFailsOnAlreadyCreatedOne(final VertxTestContext ctx) {
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
  public void testLeaveWorkspaceSucceeds(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox
            .sendMessage(new CartagoMessage.JoinWorkspace(
                TEST_AGENT_IRI,
                "test_agent",
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
  public void testLeaveWorkspaceFailsOnNotJoinedOne(final VertxTestContext ctx) {
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
  public void testCreateArtifactWithoutParametersSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var expectedCounterArtifactThingDescription =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("td/c0_counter_artifact_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox.sendMessage(new CartagoMessage.JoinWorkspace(
            TEST_AGENT_IRI,
            TEST_AGENT_BODY,
            MAIN_WORKSPACE_NAME
        )))
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
        .onSuccess(r -> assertEqualsThingDescriptions(
            expectedCounterArtifactThingDescription,
            r.body()
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testCreateArtifactWithParametersSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var expectedCounterArtifactThingDescription =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("td/c1_counter_artifact_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox
            .sendMessage(new CartagoMessage.CreateSubWorkspace(
                MAIN_WORKSPACE_NAME,
                SUB_WORKSPACE_NAME
            )))
        .compose(r -> this.cartagoMessagebox.sendMessage(new CartagoMessage.JoinWorkspace(
            TEST_AGENT_IRI,
            TEST_AGENT_BODY,
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
        .onSuccess(r -> assertEqualsThingDescriptions(
            expectedCounterArtifactThingDescription,
            r.body()
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testCreateArtifactWithFeedbackParameterSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var expectedAdderArtifactThingDescription =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("td/a0_adder_artifact_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox.sendMessage(new CartagoMessage.JoinWorkspace(
            TEST_AGENT_IRI,
            TEST_AGENT_BODY,
            MAIN_WORKSPACE_NAME
        )))
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
        .onSuccess(r -> assertEqualsThingDescriptions(
            expectedAdderArtifactThingDescription,
            r.body()
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testCreateArtifactFailsWithUnknownClass(final VertxTestContext ctx) {
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
  public void testCreateArtifactFailsWithUnknownWorkspace(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox.sendMessage(new CartagoMessage.JoinWorkspace(
            TEST_AGENT_IRI,
            TEST_AGENT_BODY,
            MAIN_WORKSPACE_NAME
        )))
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
            HttpStatus.SC_NOT_FOUND,
            ((ReplyException) t).failureCode(),
            OPERATION_FAIL_MESSAGE
        ))
        .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testCreateArtifactFailsWithWrongParameters(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox.sendMessage(new CartagoMessage.JoinWorkspace(
            TEST_AGENT_IRI,
            TEST_AGENT_BODY,
            MAIN_WORKSPACE_NAME
        )))
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
  public void testFocusAgentBody(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var expectedBodyThingDescription =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("td/test_agent_body_focus_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox
            .sendMessage(new CartagoMessage.JoinWorkspace(
                FOCUSING_AGENT_IRI,
                TEST_AGENT_BODY,
                MAIN_WORKSPACE_NAME
            )))
        .onSuccess(r -> {
          assertEqualsThingDescriptions(
              expectedBodyThingDescription,
              r.body()
          );
          this.cartagoMessagebox.sendMessage(new CartagoMessage.Focus(
                  FOCUSING_AGENT_IRI,
                  MAIN_WORKSPACE_NAME,
                  "body_test_agent"
              )).onSuccess(rx -> {
                Assertions.assertEquals("200", rx.body(), OPERATION_SUCCESS_MESSAGE);
                ctx.completeNow();
              })
              .onFailure(fx -> ctx.failNow("Failed to focus on Agent body"));
        });
  }

  @Test
  public void testFocusSucceeds(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox.sendMessage(new CartagoMessage.JoinWorkspace(
            FOCUSING_AGENT_IRI,
            TEST_AGENT_BODY,
            MAIN_WORKSPACE_NAME
        )))
        .compose(r -> this.cartagoMessagebox
            .sendMessage(new CartagoMessage.CreateArtifact(
                FOCUSING_AGENT_IRI,
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
  public void testFocusFailsWithNonexistentWorkspace(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox.sendMessage(new CartagoMessage.JoinWorkspace(
            FOCUSING_AGENT_IRI,
            TEST_AGENT_BODY,
            MAIN_WORKSPACE_NAME
        )))
        .compose(r -> this.cartagoMessagebox
            .sendMessage(new CartagoMessage.CreateArtifact(
                FOCUSING_AGENT_IRI,
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
            HttpStatus.SC_NOT_FOUND,
            ((ReplyException) t).failureCode(),
            OPERATION_FAIL_MESSAGE
        ))
        .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testFocusFailsWithNonexistentArtifactName(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox.sendMessage(new CartagoMessage.JoinWorkspace(
            FOCUSING_AGENT_IRI,
            TEST_AGENT_BODY,
            MAIN_WORKSPACE_NAME
        )))
        .compose(r -> this.cartagoMessagebox
            .sendMessage(new CartagoMessage.CreateArtifact(
                FOCUSING_AGENT_IRI,
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
            HttpStatus.SC_NOT_FOUND,
            ((ReplyException) t).failureCode(),
            OPERATION_FAIL_MESSAGE
        ))
        .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testFocusIsIdempotent(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox.sendMessage(new CartagoMessage.JoinWorkspace(
            FOCUSING_AGENT_IRI,
            TEST_AGENT_BODY,
            MAIN_WORKSPACE_NAME
        )))
        .compose(r -> this.cartagoMessagebox
            .sendMessage(new CartagoMessage.CreateArtifact(
                FOCUSING_AGENT_IRI,
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
  public void testDoActionSucceeds(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox.sendMessage(new CartagoMessage.JoinWorkspace(
            TEST_AGENT_IRI,
            TEST_AGENT_BODY,
            MAIN_WORKSPACE_NAME
        )))
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
                Optional.empty(),
                r.body(),
                ctx.toString()
            )))
        .onSuccess(r -> Assertions.assertNull(r.body(), OPERATION_SUCCESS_MESSAGE))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testDoActionAfterFocusSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var COUNTER_ARTIFACT_TD =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("td/counter_artifact_td.ttl").toURI()),
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
            .sendMessage(new CartagoMessage.JoinWorkspace(
                FOCUSING_AGENT_IRI,
                TEST_AGENT_BODY,
                SUB_WORKSPACE_NAME
            )))
        .compose(r -> this.cartagoMessagebox
            .sendMessage(new CartagoMessage.CreateArtifact(
                FOCUSING_AGENT_IRI,
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
              FOCUSING_AGENT_IRI,
              SUB_WORKSPACE_NAME,
              "c1",
              "POSThttp://localhost:8080/workspaces/sub/artifacts/c1/increment",
              Optional.empty(),
              COUNTER_ARTIFACT_TD,
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
  public void testDoActionWithFeedbackParameterSucceeds(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox.sendMessage(new CartagoMessage.JoinWorkspace(
            TEST_AGENT_IRI,
            TEST_AGENT_BODY,
            MAIN_WORKSPACE_NAME
        )))
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
                    Optional.empty(),
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
  public void testDoActionWithMultipleFeedbackParameterSucceeds(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox.sendMessage(new CartagoMessage.JoinWorkspace(
            TEST_AGENT_IRI,
            TEST_AGENT_BODY,
            MAIN_WORKSPACE_NAME
        )))
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
                    Optional.empty(),
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
  public void testDoActionSucceedsWithNoPayloadButOneFeedBackParams(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox.sendMessage(new CartagoMessage.JoinWorkspace(
            TEST_AGENT_IRI,
            TEST_AGENT_BODY,
            MAIN_WORKSPACE_NAME
        )))
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
                    Optional.empty(),
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
        .compose(r -> this.cartagoMessagebox.sendMessage(new CartagoMessage.JoinWorkspace(
            TEST_AGENT_IRI,
            TEST_AGENT_BODY,
            MAIN_WORKSPACE_NAME
        )))
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
                    Optional.empty(),
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
  public void testDoActionFailsWithNonexistentWorkspace(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox.sendMessage(new CartagoMessage.JoinWorkspace(
            TEST_AGENT_IRI,
            TEST_AGENT_BODY,
            MAIN_WORKSPACE_NAME
        )))
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
                Optional.empty(),
                r.body(),
                Optional.of(CartagoDataBundle.toJson(List.of(2, 2))).toString()
            )))
        .onFailure(t -> Assertions.assertEquals(
            HttpStatus.SC_NOT_FOUND,
            ((ReplyException) t).failureCode(),
            OPERATION_FAIL_MESSAGE
        ))
        .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testDoActionFailsWithNonexistentArtifact(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox.sendMessage(new CartagoMessage.JoinWorkspace(
            TEST_AGENT_IRI,
            TEST_AGENT_BODY,
            MAIN_WORKSPACE_NAME
        )))
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
                Optional.empty(),
                r.body(),
                Optional.of(CartagoDataBundle.toJson(List.of(2, 2))).toString()
            )))
        .onFailure(t -> Assertions.assertEquals(
            HttpStatus.SC_NOT_FOUND,
            ((ReplyException) t).failureCode(),
            OPERATION_FAIL_MESSAGE
        ))
        .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testDoActionFailsWithNonexistentOperation(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox.sendMessage(new CartagoMessage.JoinWorkspace(
            TEST_AGENT_IRI,
            TEST_AGENT_BODY,
            MAIN_WORKSPACE_NAME
        )))
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
                Optional.empty(),
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
  public void testDoActionFailsWithWrongParameters(final VertxTestContext ctx) {
    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.CreateWorkspace(MAIN_WORKSPACE_NAME))
        .compose(r -> this.cartagoMessagebox.sendMessage(new CartagoMessage.JoinWorkspace(
            TEST_AGENT_IRI,
            TEST_AGENT_BODY,
            MAIN_WORKSPACE_NAME
        )))
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
                Optional.empty(),
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
        getArtifactsIriFromWorkspace(workspace) + artifact,
        notifyPropertyMessage.requestIri(),
        URIS_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        notifyPropertyMessage.content(),
        content,
        "The properties should be equal"
    );
  }

  private void assertEqualsThingDescriptions(final String expected, final String actual) {
    final var theSame = Models.isomorphic(
        TDGraphReader.readFromString(ThingDescription.TDFormat.RDF_TURTLE, expected).getGraph()
            .orElseThrow(),
        TDGraphReader.readFromString(ThingDescription.TDFormat.RDF_TURTLE, actual).getGraph()
            .orElseThrow()
    );
    if (!theSame) {
      System.out.println(actual);
    }
    Assertions.assertTrue(
        theSame,
        TDS_EQUAL_MESSAGE
    );
  }
}
