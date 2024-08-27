package org.hyperagents.yggdrasil;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hyperagents.yggdrasil.TConstants.*;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@ExtendWith(VertxExtension.class)
public class BodyNotificationTest {

  private List<Promise<Map.Entry<String, String>>> callbackMessages;
  private WebClient client;
  private int promiseIndex;

  @BeforeEach
  public void setUp(final Vertx vertx, final VertxTestContext ctx,final TestInfo testInfo) {
    final JsonObject env;
    final String testName = testInfo.getTestMethod().orElseThrow().getName();
    if(testName.contains(TD.toUpperCase(Locale.ENGLISH))) {
      env =TDEnv;
    } else if (testName.contains(HMAS.toUpperCase(Locale.ENGLISH))) {
      env =HMASEnv;
    } else {
      throw new RuntimeException(ONTOLOGY_SPECIFIED_MESSAGE);
    }
    this.client = WebClient.create(vertx);
    this.callbackMessages =
      Stream.generate(Promise::<Map.Entry<String, String>>promise)
            .limit(5)
            .collect(Collectors.toList());
    this.promiseIndex = 0;
    vertx
        .eventBus()
        .<String>consumer(
          "test",
          m -> {
            this.callbackMessages
                .get(this.promiseIndex)
                .complete(Map.entry(m.headers().get("entityIri"), m.body()));
            this.promiseIndex++;
          }
        );
    vertx.deployVerticle(new CallbackServerVerticle(8081))
         .compose(r -> vertx.deployVerticle(
           new MainVerticle(),
           new DeploymentOptions().setConfig(JsonObject.of(
             HTTP_CONFIG,
             JsonObject.of(
               "host",
               TEST_HOST,
               "port",
               TEST_PORT
             ),
             NOTIFICATION_CONFIG,
             JsonObject.of(
               ENABLED,
               true
             ),
             ENVIRONMENT_CONFIG,
             env
           ))
         ))
         .onComplete(ctx.succeedingThenComplete());
  }

  @AfterEach
  public void tearDown(final Vertx vertx, final VertxTestContext ctx) {
    vertx.close().onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testRunTD(final VertxTestContext ctx) throws URISyntaxException, IOException {
    final var workspaceRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("td/output_test_workspace_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    final var artifactRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("td/c0_counter_artifact_test_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    final var testAgentBodyRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("td/test_agent_body_test_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    final var workspaceWithArtifactAndBodyRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("td/test_workspace_c0_body_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    this.client
        .post(TEST_PORT, TEST_HOST, WORKSPACES_PATH)
        .putHeader(AGENT_ID_HEADER, TEST_AGENT_ID)
        .putHeader(HINT_HEADER, MAIN_WORKSPACE_NAME)
        .send()
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_CREATED,
              r.statusCode(),
              CREATED_STATUS_MESSAGE
          );
          assertEqualsThingDescriptions(
              workspaceRepresentation,
              r.bodyAsString()
          );
        })
        .compose(r -> this.client
                          .post(
                            TEST_PORT,
                            TEST_HOST,
                            WORKSPACES_PATH + MAIN_WORKSPACE_NAME + ARTIFACTS_PATH
                          )
                          .putHeader(AGENT_ID_HEADER, TEST_AGENT_ID)
                          .sendJsonObject(JsonObject.of(
                            ARTIFACT_NAME,
                            COUNTER_ARTIFACT_NAME,
                            ARTIFACT_CLASS,
                            COUNTER_ARTIFACT_CLASS
                          )))
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_CREATED,
              r.statusCode(),
              CREATED_STATUS_MESSAGE
          );
          assertEqualsThingDescriptions(
              artifactRepresentation,
              r.bodyAsString()
          );
        })
        .compose(r -> this.client
                          .post(TEST_PORT, TEST_HOST, HUB_PATH)
                          .sendJsonObject(JsonObject.of(
                            HUB_MODE_PARAM,
                            HUB_MODE_SUBSCRIBE,
                            HUB_TOPIC_PARAM,
                            this.getUrl(WORKSPACES_PATH + MAIN_WORKSPACE_NAME),
                            HUB_CALLBACK_PARAM,
                            CALLBACK_URL
                          )))
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );
          Assertions.assertNull(r.body(), RESPONSE_BODY_EMPTY_MESSAGE);
        })
        .compose(r -> this.client
                          .post(TEST_PORT, TEST_HOST, HUB_PATH)
                          .sendJsonObject(JsonObject.of(
                            HUB_MODE_PARAM,
                            HUB_MODE_SUBSCRIBE,
                            HUB_TOPIC_PARAM,
                            this.getUrl(
                              WORKSPACES_PATH
                              + MAIN_WORKSPACE_NAME
                              + ARTIFACTS_PATH
                            ),
                            HUB_CALLBACK_PARAM,
                            CALLBACK_URL
                          )))
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );
          Assertions.assertNull(r.body(), RESPONSE_BODY_EMPTY_MESSAGE);
        })
        .compose(r -> this.client
                          .post(
                            TEST_PORT,
                            TEST_HOST,
                            WORKSPACES_PATH + MAIN_WORKSPACE_NAME + "/join"
                          )
                          .putHeader(AGENT_ID_HEADER, TEST_AGENT_ID)
          .putHeader(AGENT_LOCALNAME_HEADER, TEST_AGENT_NAME)
                          .send())
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );
          assertEqualsThingDescriptions(
              testAgentBodyRepresentation,
              r.bodyAsString()
          );
        })
        .compose(r -> this.callbackMessages.getFirst().future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              this.getUrl(WORKSPACES_PATH + MAIN_WORKSPACE_NAME),
              m.getKey(),
              URIS_EQUAL_MESSAGE
          );
         assertEqualsThingDescriptions(
              workspaceWithArtifactAndBodyRepresentation,
              m.getValue()
          );
        })
        .compose(r -> this.callbackMessages.get(1).future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              this.getUrl(WORKSPACES_PATH + MAIN_WORKSPACE_NAME + ARTIFACTS_PATH),
              m.getKey(),
              URIS_EQUAL_MESSAGE
          );
          assertEqualsThingDescriptions(
              testAgentBodyRepresentation,
              m.getValue()
          );
        })
        .compose(r -> this.client
                          .post(TEST_PORT, TEST_HOST, HUB_PATH)
                          .sendJsonObject(JsonObject.of(
                            HUB_MODE_PARAM,
                            HUB_MODE_SUBSCRIBE,
                            HUB_TOPIC_PARAM,
                            this.getUrl(
                              WORKSPACES_PATH
                              + MAIN_WORKSPACE_NAME
                              + ARTIFACTS_PATH
                              + TEST_AGENT_BODY_NAME
                              + "/"
                            ),
                            HUB_CALLBACK_PARAM,
                            CALLBACK_URL
                          )))
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );
          Assertions.assertNull(r.body(), RESPONSE_BODY_EMPTY_MESSAGE);
        })
        .compose(r ->
          this.client
              .post(
                TEST_PORT,
                TEST_HOST,
                WORKSPACES_PATH
                  + MAIN_WORKSPACE_NAME
                  + ARTIFACTS_PATH
                  + COUNTER_ARTIFACT_NAME
                  + "/increment"
              )
              .putHeader(AGENT_ID_HEADER, TEST_AGENT_ID)
              .send())
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );
          Assertions.assertNull(r.bodyAsString(), RESPONSE_BODY_EMPTY_MESSAGE);
        })
        .compose(r -> this.callbackMessages.get(2).future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              this.getUrl(
                WORKSPACES_PATH
                + MAIN_WORKSPACE_NAME
                + ARTIFACTS_PATH
                + TEST_AGENT_BODY_NAME
                + "/"
              ),
              m.getKey(),
              URIS_EQUAL_MESSAGE
          );
          Assertions.assertEquals(
              JsonObject
                .of(
                  ARTIFACT_NAME,
                  COUNTER_ARTIFACT_NAME,
                  ACTION_NAME,
                  COUNTER_ARTIFACT_ACTION_NAME,
                  EVENT_TYPE,
                  "actionRequested"
                )
                .encode(),
              m.getValue(),
              REPRESENTATIONS_EQUAL_MESSAGE
          );
        })
        .compose(r -> this.callbackMessages.get(3).future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              this.getUrl(
                WORKSPACES_PATH
                + MAIN_WORKSPACE_NAME
                + ARTIFACTS_PATH
                + TEST_AGENT_BODY_NAME
                + "/"
              ),
              m.getKey(),
              URIS_EQUAL_MESSAGE
          );
          Assertions.assertEquals(
              JsonObject
                .of(
                  ARTIFACT_NAME,
                  COUNTER_ARTIFACT_NAME,
                  ACTION_NAME,
                  COUNTER_ARTIFACT_ACTION_NAME,
                  EVENT_TYPE,
                  "actionSucceeded"
                )
                .encode(),
              m.getValue(),
              REPRESENTATIONS_EQUAL_MESSAGE
          );
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testRunHMAS(final VertxTestContext ctx) throws URISyntaxException, IOException {
    final var workspaceRepresentation =
      Files.readString(
        Path.of(ClassLoader.getSystemResource("hmas/output_test_workspace_hmas.ttl").toURI()),
        StandardCharsets.UTF_8
      );
    final var artifactRepresentation =
      Files.readString(
        Path.of(ClassLoader.getSystemResource("hmas/c0_counter_artifact_test_hmas.ttl").toURI()),
        StandardCharsets.UTF_8
      );
    final var testAgentBodyRepresentation =
      Files.readString(
        Path.of(ClassLoader.getSystemResource("hmas/test_agent_body_test_hmas.ttl").toURI()),
        StandardCharsets.UTF_8
      );
    final var workspaceWithArtifactAndBodyRepresentation =
      Files.readString(
        Path.of(ClassLoader.getSystemResource("hmas/test_workspace_c0_body_hmas.ttl").toURI()),
        StandardCharsets.UTF_8
      );
    this.client
      .post(TEST_PORT, TEST_HOST, WORKSPACES_PATH)
      .putHeader(AGENT_ID_HEADER, TEST_AGENT_ID)
      .putHeader(HINT_HEADER, MAIN_WORKSPACE_NAME)
      .send()
      .onSuccess(r -> {
        Assertions.assertEquals(
          HttpStatus.SC_CREATED,
          r.statusCode(),
          CREATED_STATUS_MESSAGE
        );
        assertEqualsHMASDescriptions(
          workspaceRepresentation,
          r.bodyAsString()
        );
      })
      .compose(r -> this.client
        .post(
          TEST_PORT,
          TEST_HOST,
          WORKSPACES_PATH + MAIN_WORKSPACE_NAME + ARTIFACTS_PATH
        )
        .putHeader(AGENT_ID_HEADER, TEST_AGENT_ID)
        .sendJsonObject(JsonObject.of(
          ARTIFACT_NAME,
          COUNTER_ARTIFACT_NAME,
          ARTIFACT_CLASS,
          COUNTER_ARTIFACT_CLASS
        )))
      .onSuccess(r -> {
        Assertions.assertEquals(
          HttpStatus.SC_CREATED,
          r.statusCode(),
          CREATED_STATUS_MESSAGE
        );
        assertEqualsHMASDescriptions(
          artifactRepresentation,
          r.bodyAsString()
        );
      })
      .compose(r -> this.client
        .post(TEST_PORT, TEST_HOST, HUB_PATH)
        .sendJsonObject(JsonObject.of(
          HUB_MODE_PARAM,
          HUB_MODE_SUBSCRIBE,
          HUB_TOPIC_PARAM,
          this.getUrl(WORKSPACES_PATH + MAIN_WORKSPACE_NAME),
          HUB_CALLBACK_PARAM,
          CALLBACK_URL
        )))
      .onSuccess(r -> {
        Assertions.assertEquals(
          HttpStatus.SC_OK,
          r.statusCode(),
          OK_STATUS_MESSAGE
        );
        Assertions.assertNull(r.body(), RESPONSE_BODY_EMPTY_MESSAGE);
      })
      .compose(r -> this.client
        .post(TEST_PORT, TEST_HOST, HUB_PATH)
        .sendJsonObject(JsonObject.of(
          HUB_MODE_PARAM,
          HUB_MODE_SUBSCRIBE,
          HUB_TOPIC_PARAM,
          this.getUrl(
            WORKSPACES_PATH
              + MAIN_WORKSPACE_NAME
              + ARTIFACTS_PATH
          ),
          HUB_CALLBACK_PARAM,
          CALLBACK_URL
        )))
      .onSuccess(r -> {
        Assertions.assertEquals(
          HttpStatus.SC_OK,
          r.statusCode(),
          OK_STATUS_MESSAGE
        );
        Assertions.assertNull(r.body(), RESPONSE_BODY_EMPTY_MESSAGE);
      })
      .compose(r -> this.client
        .post(
          TEST_PORT,
          TEST_HOST,
          WORKSPACES_PATH + MAIN_WORKSPACE_NAME + "/join"
        )
        .putHeader(AGENT_ID_HEADER, TEST_AGENT_ID)
        .putHeader(AGENT_LOCALNAME_HEADER, TEST_AGENT_NAME)
        .send())
      .onSuccess(r -> {
        Assertions.assertEquals(
          HttpStatus.SC_OK,
          r.statusCode(),
          OK_STATUS_MESSAGE
        );
        assertEqualsHMASDescriptions(
          testAgentBodyRepresentation,
          r.bodyAsString()
        );
      })
      .compose(r -> this.callbackMessages.getFirst().future())
      .onSuccess(m -> {
        Assertions.assertEquals(
          this.getUrl(WORKSPACES_PATH + MAIN_WORKSPACE_NAME),
          m.getKey(),
          URIS_EQUAL_MESSAGE
        );
        assertEqualsHMASDescriptions(
          workspaceWithArtifactAndBodyRepresentation,
          m.getValue()
        );
      })
      .compose(r -> this.callbackMessages.get(1).future())
      .onSuccess(m -> {
        Assertions.assertEquals(
          this.getUrl(WORKSPACES_PATH + MAIN_WORKSPACE_NAME + ARTIFACTS_PATH),
          m.getKey(),
          URIS_EQUAL_MESSAGE
        );
        assertEqualsHMASDescriptions(
          testAgentBodyRepresentation,
          m.getValue()
        );
      })
      .compose(r -> this.client
        .post(TEST_PORT, TEST_HOST, HUB_PATH)
        .sendJsonObject(JsonObject.of(
          HUB_MODE_PARAM,
          HUB_MODE_SUBSCRIBE,
          HUB_TOPIC_PARAM,
          this.getUrl(
            WORKSPACES_PATH
              + MAIN_WORKSPACE_NAME
              + ARTIFACTS_PATH
              + TEST_AGENT_BODY_NAME
              + "/"
          ),
          HUB_CALLBACK_PARAM,
          CALLBACK_URL
        )))
      .onSuccess(r -> {
        Assertions.assertEquals(
          HttpStatus.SC_OK,
          r.statusCode(),
          OK_STATUS_MESSAGE
        );
        Assertions.assertNull(r.body(), RESPONSE_BODY_EMPTY_MESSAGE);
      })
      .compose(r ->
        this.client
          .post(
            TEST_PORT,
            TEST_HOST,
            WORKSPACES_PATH
              + MAIN_WORKSPACE_NAME
              + ARTIFACTS_PATH
              + COUNTER_ARTIFACT_NAME
              + "/increment"
          )
          .putHeader(AGENT_ID_HEADER, TEST_AGENT_ID)
          .send())
      .onSuccess(r -> {
        Assertions.assertEquals(
          HttpStatus.SC_OK,
          r.statusCode(),
          OK_STATUS_MESSAGE
        );
        Assertions.assertNull(r.bodyAsString(), RESPONSE_BODY_EMPTY_MESSAGE);
      })
      .compose(r -> this.callbackMessages.get(2).future())
      .onSuccess(m -> {
        Assertions.assertEquals(
          this.getUrl(
            WORKSPACES_PATH
              + MAIN_WORKSPACE_NAME
              + ARTIFACTS_PATH
              + TEST_AGENT_BODY_NAME
              + "/"
          ),
          m.getKey(),
          URIS_EQUAL_MESSAGE
        );
        Assertions.assertEquals(
          JsonObject
            .of(
              ARTIFACT_NAME,
              COUNTER_ARTIFACT_NAME,
              ACTION_NAME,
              COUNTER_ARTIFACT_ACTION_NAME,
              EVENT_TYPE,
              "actionRequested"
            )
            .encode(),
          m.getValue(),
          REPRESENTATIONS_EQUAL_MESSAGE
        );
      })
      .compose(r -> this.callbackMessages.get(3).future())
      .onSuccess(m -> {
        Assertions.assertEquals(
          this.getUrl(
            WORKSPACES_PATH
              + MAIN_WORKSPACE_NAME
              + ARTIFACTS_PATH
              + TEST_AGENT_BODY_NAME
              + "/"
          ),
          m.getKey(),
          URIS_EQUAL_MESSAGE
        );
        Assertions.assertEquals(
          JsonObject
            .of(
              ARTIFACT_NAME,
              COUNTER_ARTIFACT_NAME,
              ACTION_NAME,
              COUNTER_ARTIFACT_ACTION_NAME,
              EVENT_TYPE,
              "actionSucceeded"
            )
            .encode(),
          m.getValue(),
          REPRESENTATIONS_EQUAL_MESSAGE
        );
      })
      .onComplete(ctx.succeedingThenComplete());
  }


  private String getUrl(final String path) {
    return "http://" + TEST_HOST + ":" + TEST_PORT + path;
  }

}
