package org.hyperagents.yggdrasil.configuration;

import static org.hyperagents.yggdrasil.TConstants.ARTIFACTS_PATH;
import static org.hyperagents.yggdrasil.TConstants.COUNTER_ARTIFACT_NAME;
import static org.hyperagents.yggdrasil.TConstants.COUNTER_ARTIFACT_URI;
import static org.hyperagents.yggdrasil.TConstants.OK_STATUS_MESSAGE;
import static org.hyperagents.yggdrasil.TConstants.REPRESENTATIONS_EQUAL_MESSAGE;
import static org.hyperagents.yggdrasil.TConstants.SUB_WORKSPACE_NAME;
import static org.hyperagents.yggdrasil.TConstants.TEST_HOST;
import static org.hyperagents.yggdrasil.TConstants.TEST_PORT;
import static org.hyperagents.yggdrasil.TConstants.URIS_EQUAL_MESSAGE;
import static org.hyperagents.yggdrasil.TConstants.WORKSPACES_PATH;
import static org.hyperagents.yggdrasil.TConstants.assertEqualsHMASDescriptions;
import static org.hyperagents.yggdrasil.TConstants.assertEqualsThingDescriptions;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
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
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.hc.core5.http.HttpStatus;
import org.hyperagents.yggdrasil.CallbackServerVerticle;
import org.hyperagents.yggdrasil.MainVerticle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * testing different environment configs.
 */
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@ExtendWith(VertxExtension.class)
public class EnvironmentConfigurationTest {
  private List<Promise<Map.Entry<String, String>>> callbackMessages;
  private WebClient client;
  private int promiseIndex;

  /**
   * setup method.
   *
   * @param vertx vertx
   * @param ctx ctx
   * @param testInfo testInfo
   * @throws URISyntaxException exceptions
   * @throws IOException exceptions
   */
  @BeforeEach
  public void setUp(final Vertx vertx, final VertxTestContext ctx, final TestInfo testInfo)
      throws URISyntaxException, IOException {
    this.client = WebClient.create(vertx);
    this.callbackMessages =
        Stream.generate(Promise::<Map.Entry<String, String>>promise)
            .limit(2)
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
    final String testName = testInfo.getTestMethod().orElseThrow().getName();
    final String conf;
    if (testName.contains("TD")) {
      conf = "td/cartago_config.json";
    } else if (testName.contains("HMAS")) {
      conf = "hmas/cartago_config.json";
    } else {
      throw new RuntimeException("Test did not specify ontology");
    }
    final var configuration = Files.readString(
        Path.of(ClassLoader.getSystemResource(conf).toURI()),
        StandardCharsets.UTF_8
    );
    vertx.deployVerticle(new CallbackServerVerticle(8081))
        .compose(r -> vertx.deployVerticle(
            new MainVerticle(),
            new DeploymentOptions().setConfig((JsonObject) Json.decodeValue(configuration))
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @AfterEach
  public void tearDown(final Vertx vertx, final VertxTestContext ctx) {
    vertx.close().onComplete(ctx.succeedingThenComplete());
    this.client.close();
  }

  @Test
  public void testRunTD(final VertxTestContext ctx) throws URISyntaxException, IOException {
    final var workspaceRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("td/test_workspace_sub_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var artifactRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("td/c0_counter_artifact_sub_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var subWorkspaceRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("td/sub_workspace_c0_body.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    this.client
        .get(TEST_PORT, TEST_HOST, WORKSPACES_PATH + "test")
        .send()
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );
          assertEqualsThingDescriptions(
              workspaceRepresentation,
              r.bodyAsString()
          );
        })
        .compose(r -> this.client
            .get(TEST_PORT, TEST_HOST, WORKSPACES_PATH + SUB_WORKSPACE_NAME)
            .send())
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );
          assertEqualsThingDescriptions(
              subWorkspaceRepresentation,
              r.bodyAsString()
          );
        })
        .compose(r -> this.client
            .get(
                TEST_PORT,
                TEST_HOST,
                WORKSPACES_PATH
                    + SUB_WORKSPACE_NAME
                    + ARTIFACTS_PATH
                    + COUNTER_ARTIFACT_NAME
            )
            .send())
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );
          assertEqualsThingDescriptions(
              artifactRepresentation,
              r.bodyAsString()
          );
        })
        .compose(r -> this.callbackMessages.getFirst().future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              COUNTER_ARTIFACT_URI,
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
            .putHeader("X-Agent-WebID", "http://localhost:8080/agents/test")
            .send())
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );
          Assertions.assertNull(r.bodyAsString(), "The response body should be empty");
        })
        .compose(r -> this.callbackMessages.get(1).future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              COUNTER_ARTIFACT_URI,
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

  @Test
  public void testRunHMAS(final VertxTestContext ctx) throws URISyntaxException, IOException {
    final var workspaceRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("hmas/test_workspace_sub_hmas.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var artifactRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("hmas/c0_counter_artifact_sub_hmas.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var subWorkspaceRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("hmas/sub_workspace_c0_body.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    this.client
        .get(TEST_PORT, TEST_HOST, WORKSPACES_PATH + "test")
        .send()
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );

          assertEqualsHMASDescriptions(
              workspaceRepresentation,
              r.bodyAsString()
          );
        })
        .compose(r -> this.client
            .get(TEST_PORT, TEST_HOST, WORKSPACES_PATH + SUB_WORKSPACE_NAME)
            .send())
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );

          assertEqualsHMASDescriptions(
              subWorkspaceRepresentation,
              r.bodyAsString()
          );
        })
        .compose(r -> this.client
            .get(
                TEST_PORT,
                TEST_HOST,
                WORKSPACES_PATH
                    + SUB_WORKSPACE_NAME
                    + ARTIFACTS_PATH
                    + COUNTER_ARTIFACT_NAME
            )
            .send())
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );
          assertEqualsHMASDescriptions(
              artifactRepresentation,
              r.bodyAsString()
          );
        })
        .compose(r -> this.callbackMessages.getFirst().future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              COUNTER_ARTIFACT_URI,
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
            .putHeader("X-Agent-WebID", "http://localhost:8080/agents/test")
            .send())
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );
          Assertions.assertNull(r.bodyAsString(), "The response body should be empty");
        })
        .compose(r -> this.callbackMessages.get(1).future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              COUNTER_ARTIFACT_URI,
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
}
