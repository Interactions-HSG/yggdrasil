package org.hyperagents.yggdrasil;

import static org.hyperagents.yggdrasil.TConstants.AGENT_ID_HEADER;
import static org.hyperagents.yggdrasil.TConstants.AGENT_LOCALNAME_HEADER;
import static org.hyperagents.yggdrasil.TConstants.ARTIFACTS_PATH;
import static org.hyperagents.yggdrasil.TConstants.ARTIFACT_CLASS;
import static org.hyperagents.yggdrasil.TConstants.ARTIFACT_NAME;
import static org.hyperagents.yggdrasil.TConstants.CALLBACK_URL;
import static org.hyperagents.yggdrasil.TConstants.COUNTER_ARTIFACT_CLASS;
import static org.hyperagents.yggdrasil.TConstants.COUNTER_ARTIFACT_NAME;
import static org.hyperagents.yggdrasil.TConstants.CREATED_STATUS_MESSAGE;
import static org.hyperagents.yggdrasil.TConstants.ENABLED;
import static org.hyperagents.yggdrasil.TConstants.ENVIRONMENT_CONFIG;
import static org.hyperagents.yggdrasil.TConstants.HINT_HEADER;
import static org.hyperagents.yggdrasil.TConstants.HMAS;
import static org.hyperagents.yggdrasil.TConstants.HMASEnv;
import static org.hyperagents.yggdrasil.TConstants.HTTP_CONFIG;
import static org.hyperagents.yggdrasil.TConstants.HUB_CALLBACK_PARAM;
import static org.hyperagents.yggdrasil.TConstants.HUB_MODE_PARAM;
import static org.hyperagents.yggdrasil.TConstants.HUB_MODE_SUBSCRIBE;
import static org.hyperagents.yggdrasil.TConstants.HUB_PATH;
import static org.hyperagents.yggdrasil.TConstants.HUB_TOPIC_PARAM;
import static org.hyperagents.yggdrasil.TConstants.INIT_PARAMS;
import static org.hyperagents.yggdrasil.TConstants.MAIN_WORKSPACE_NAME;
import static org.hyperagents.yggdrasil.TConstants.NOTIFICATION_CONFIG;
import static org.hyperagents.yggdrasil.TConstants.OK_STATUS_MESSAGE;
import static org.hyperagents.yggdrasil.TConstants.ONTOLOGY_SPECIFIED_MESSAGE;
import static org.hyperagents.yggdrasil.TConstants.REPRESENTATIONS_EQUAL_MESSAGE;
import static org.hyperagents.yggdrasil.TConstants.RESPONSE_BODY_EMPTY_MESSAGE;
import static org.hyperagents.yggdrasil.TConstants.SUBSCRIBE_WORKSPACE_PATH;
import static org.hyperagents.yggdrasil.TConstants.SUB_WORKSPACE_NAME;
import static org.hyperagents.yggdrasil.TConstants.TD;
import static org.hyperagents.yggdrasil.TConstants.TDEnv;
import static org.hyperagents.yggdrasil.TConstants.TEST_AGENT_ID;
import static org.hyperagents.yggdrasil.TConstants.TEST_AGENT_NAME;
import static org.hyperagents.yggdrasil.TConstants.TEST_HOST;
import static org.hyperagents.yggdrasil.TConstants.TEST_PORT;
import static org.hyperagents.yggdrasil.TConstants.URIS_EQUAL_MESSAGE;
import static org.hyperagents.yggdrasil.TConstants.WORKSPACES_PATH;

import com.google.common.net.HttpHeaders;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Tests the main functionality -> system tests.
 */
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.JUnitAssertionsShouldIncludeMessage"})
@ExtendWith(VertxExtension.class)
public class MainVerticleTest {


  private List<Promise<Map.Entry<String, String>>> callbackMessages;
  private WebClient client;
  private int promiseIndex;

  /**
   * setup method.
   *
   * @param vertx    vertx
   * @param ctx      ctx
   * @param testInfo testInfo
   */
  @BeforeEach
  public void setUp(final Vertx vertx, final VertxTestContext ctx, final TestInfo testInfo) {

    final JsonObject env;
    final String testName = testInfo.getTestMethod().orElseThrow().getName();
    if (testName.contains(TD.toUpperCase(Locale.ENGLISH))) {
      env = TDEnv;
    } else if (testName.contains(HMAS.toUpperCase(Locale.ENGLISH))) {
      env = HMASEnv;
    } else {
      throw new RuntimeException(ONTOLOGY_SPECIFIED_MESSAGE);
    }

    this.client = WebClient.create(vertx);
    this.callbackMessages =
        Stream.generate(Promise::<Map.Entry<String, String>>promise)
            .limit(9)
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
    final var platformRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("td/platform_test_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var workspaceRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("td/output_test_workspace_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var subWorkspaceRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("td/output_sub_workspace_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var workspaceWithSubWorkspaceRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("td/test_workspace_sub_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var websubArtifactsRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource(
                "td/platform_test_websub_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var websubArtifactsTwoRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource(
                "td/test_workspace_websub_workspaces.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var websubArtifactsThreeRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource(
                "td/sub_websub_update_new_artifact_two.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var artifactRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("td/c0_counter_artifact_sub_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var subWorkspaceWithArtifactAndBodyRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("td/sub_workspace_c0_body.ttl").toURI()),
            StandardCharsets.UTF_8
        );

    testHelper(
        ctx,
        platformRepresentation,
        workspaceRepresentation,
        subWorkspaceRepresentation,
        workspaceWithSubWorkspaceRepresentation,
        websubArtifactsRepresentation,
        websubArtifactsTwoRepresentation,
        websubArtifactsThreeRepresentation,
        artifactRepresentation,
        subWorkspaceWithArtifactAndBodyRepresentation,
        TConstants::assertEqualsThingDescriptions
    );
  }

  @Test
  public void testRunHMAS(final VertxTestContext ctx) throws URISyntaxException, IOException {
    final var platformRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("hmas/platform_test_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var workspaceRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("hmas/output_test_workspace_hmas.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var subWorkspaceRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("hmas/output_sub_workspace_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var workspaceWithSubWorkspaceRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("hmas/test_workspace_sub_hmas.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var websubArtifactsRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource(
                "td/platform_test_websub_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var websubArtifactsTwoRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource(
                "td/test_workspace_websub_workspaces.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var websubArtifactsThreeRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource(
                "td/sub_websub_update_new_artifact_two.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var artifactRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("hmas/c0_counter_artifact_sub_hmas.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var subWorkspaceWithArtifactAndBodyRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("hmas/sub_workspace_c0_body.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    testHelper(
        ctx,
        platformRepresentation,
        workspaceRepresentation,
        subWorkspaceRepresentation,
        workspaceWithSubWorkspaceRepresentation,
        websubArtifactsRepresentation,
        websubArtifactsTwoRepresentation,
        websubArtifactsThreeRepresentation,
        artifactRepresentation,
        subWorkspaceWithArtifactAndBodyRepresentation,
        TConstants::assertEqualsHMASDescriptions
    );

  }


  private void testHelper(
      final VertxTestContext ctx,
      final String platformRepresentation,
      final String workspaceRepresentation,
      final String subWorkspaceRepresentation,
      final String workspaceWithSubWorkspaceRepresentation,
      final String websubArtifactsRepresentation,
      final String websubArtifactsTwoRepresentation,
      final String websubArtifactsThreeRepresentation,
      final String artifactRepresentation,
      final String subWorkspaceWithArtifactAndBodyRepresentation,
      final BiConsumer<String, String> assertEqualsFunction
  ) {
    this.client.post(TEST_PORT, TEST_HOST, HUB_PATH)
        .sendJsonObject(JsonObject.of(
            HUB_MODE_PARAM,
            HUB_MODE_SUBSCRIBE,
            HUB_TOPIC_PARAM,
            this.getUrl("/"),
            HUB_CALLBACK_PARAM,
            CALLBACK_URL
        ))
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
                this.getUrl(WORKSPACES_PATH),
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
            .post(TEST_PORT, TEST_HOST, WORKSPACES_PATH)
            .putHeader(AGENT_ID_HEADER, TEST_AGENT_ID)
            .putHeader(HINT_HEADER, MAIN_WORKSPACE_NAME)
            .send())
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_CREATED,
              r.statusCode(),
              CREATED_STATUS_MESSAGE
          );
          assertEqualsFunction.accept(
              workspaceRepresentation,
              r.bodyAsString()
          );
        })
        .compose(r -> this.callbackMessages.getFirst().future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              this.getUrl("/"),
              m.getKey(),
              URIS_EQUAL_MESSAGE
          );
          assertEqualsFunction.accept(
              platformRepresentation,
              m.getValue()
          );
        })
        .compose(r -> this.callbackMessages.get(1).future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              this.getUrl(WORKSPACES_PATH),
              m.getKey(),
              URIS_EQUAL_MESSAGE
          );
          Assertions.assertEquals(
              websubArtifactsRepresentation.replaceAll(" ", ""),
              m.getValue().replaceAll(" ", ""));
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
        .compose(r -> this.client.post(TEST_PORT, TEST_HOST, HUB_PATH)
            .sendJsonObject(JsonObject.of(
                HUB_MODE_PARAM,
                HUB_MODE_SUBSCRIBE,
                HUB_TOPIC_PARAM,
                this.getUrl(SUBSCRIBE_WORKSPACE_PATH + MAIN_WORKSPACE_NAME),
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
            .post(TEST_PORT, TEST_HOST, WORKSPACES_PATH + MAIN_WORKSPACE_NAME)
            .putHeader(AGENT_ID_HEADER, TEST_AGENT_ID)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .putHeader(HINT_HEADER, SUB_WORKSPACE_NAME)
            .send())
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_CREATED,
              r.statusCode(),
              CREATED_STATUS_MESSAGE
          );
          assertEqualsFunction.accept(
              subWorkspaceRepresentation,
              r.bodyAsString()
          );
        })
        .compose(r -> this.callbackMessages.get(2).future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              this.getUrl(WORKSPACES_PATH + MAIN_WORKSPACE_NAME),
              m.getKey(),
              URIS_EQUAL_MESSAGE
          );
          assertEqualsFunction.accept(
              workspaceWithSubWorkspaceRepresentation,
              m.getValue()
          );
        })
        .compose(r -> this.callbackMessages.get(3).future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              this.getUrl(SUBSCRIBE_WORKSPACE_PATH + MAIN_WORKSPACE_NAME),
              m.getKey(),
              URIS_EQUAL_MESSAGE
          );
          Assertions.assertEquals(
              websubArtifactsTwoRepresentation.replaceAll(" ", ""),
              m.getValue().replaceAll(" ", ""));
        })
        .compose(r -> this.client
            .post(TEST_PORT, TEST_HOST, WORKSPACES_PATH + SUB_WORKSPACE_NAME + "/join")
            .putHeader(AGENT_ID_HEADER, TEST_AGENT_ID)
            .putHeader(AGENT_LOCALNAME_HEADER, TEST_AGENT_NAME)
            .send())
        .compose(r -> this.client
            .post(TEST_PORT, TEST_HOST, HUB_PATH)
            .sendJsonObject(JsonObject.of(
                HUB_MODE_PARAM,
                HUB_MODE_SUBSCRIBE,
                HUB_TOPIC_PARAM,
                this.getUrl(WORKSPACES_PATH + SUB_WORKSPACE_NAME),
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
                        + SUB_WORKSPACE_NAME
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
                WORKSPACES_PATH + SUB_WORKSPACE_NAME + ARTIFACTS_PATH
            )
            .putHeader(AGENT_ID_HEADER, TEST_AGENT_ID)
            .sendJsonObject(JsonObject.of(
                ARTIFACT_NAME,
                COUNTER_ARTIFACT_NAME,
                ARTIFACT_CLASS,
                COUNTER_ARTIFACT_CLASS,
                INIT_PARAMS,
                JsonArray.of(5)
            )))
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_CREATED,
              r.statusCode(),
              CREATED_STATUS_MESSAGE
          );
          assertEqualsFunction.accept(
              artifactRepresentation,
              r.bodyAsString()
          );
        })
        .compose(r -> this.callbackMessages.get(4).future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              this.getUrl(WORKSPACES_PATH + SUB_WORKSPACE_NAME),
              m.getKey(),
              URIS_EQUAL_MESSAGE
          );
          assertEqualsFunction.accept(
              subWorkspaceWithArtifactAndBodyRepresentation,
              m.getValue()
          );
        })
        .compose(r -> this.callbackMessages.get(5).future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              this.getUrl(WORKSPACES_PATH + SUB_WORKSPACE_NAME + ARTIFACTS_PATH),
              m.getKey(),
              URIS_EQUAL_MESSAGE
          );
          Assertions.assertEquals(
              websubArtifactsThreeRepresentation.replaceAll(" ", ""),
              m.getValue().replaceAll(" ", ""));
        })
        .compose(r -> this.client
            .post(
                TEST_PORT,
                TEST_HOST,
                WORKSPACES_PATH + SUB_WORKSPACE_NAME + "/focus"
            )
            .putHeader(AGENT_ID_HEADER, TEST_AGENT_ID)
            .sendJsonObject(JsonObject.of(
                ARTIFACT_NAME,
                COUNTER_ARTIFACT_NAME,
                "callbackIri",
                CALLBACK_URL
            )))
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );
          Assertions.assertEquals(
              String.valueOf(HttpStatus.SC_OK),
              r.bodyAsString(),
              OK_STATUS_MESSAGE
          );
        })
        .compose(r -> this.callbackMessages.get(6).future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              this.getUrl(
                  WORKSPACES_PATH
                      + SUB_WORKSPACE_NAME
                      + ARTIFACTS_PATH
                      + COUNTER_ARTIFACT_NAME
              ),
              m.getKey(),
              URIS_EQUAL_MESSAGE
          );
          Assertions.assertEquals(
              "count(5)",
              m.getValue(),
              REPRESENTATIONS_EQUAL_MESSAGE
          );
        })
        .compose(r -> this.client
            .post(
                TEST_PORT,
                TEST_HOST,
                WORKSPACES_PATH
                    + SUB_WORKSPACE_NAME
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
        .compose(r -> this.callbackMessages.get(7).future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              this.getUrl(
                  WORKSPACES_PATH
                      + SUB_WORKSPACE_NAME
                      + ARTIFACTS_PATH
                      + COUNTER_ARTIFACT_NAME
              ),
              m.getKey(),
              URIS_EQUAL_MESSAGE
          );
          Assertions.assertEquals(
              "count(6)",
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
