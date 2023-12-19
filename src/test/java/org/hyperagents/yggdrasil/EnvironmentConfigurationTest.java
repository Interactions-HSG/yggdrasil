package org.hyperagents.yggdrasil;

import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
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
import org.eclipse.rdf4j.model.util.Models;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@ExtendWith(VertxExtension.class)
public class EnvironmentConfigurationTest {
  private static final String TEST_AGENT_ID = "test";
  private static final String AGENT_ID_HEADER = "X-Agent-WebID";
  private static final String MAIN_WORKSPACE_NAME = "test";
  private static final String SUB_WORKSPACE_NAME = "sub";
  private static final String COUNTER_ARTIFACT_NAME = "c0";
  private static final int TEST_PORT = 8080;
  private static final String TEST_HOST = "localhost";
  private static final String OK_STATUS_MESSAGE = "Status code should be OK";
  private static final String RESPONSE_BODY_EMPTY_MESSAGE = "The response body should be empty";
  private static final String URIS_EQUAL_MESSAGE = "The URIs should be equal";
  private static final String REPRESENTATIONS_EQUAL_MESSAGE = "The representations must be equal";
  private static final String WORKSPACES_PATH = "/workspaces/";
  private static final String ARTIFACTS_PATH = "/artifacts/";

  private final List<Promise<Map.Entry<String, String>>> callbackMessages;
  private WebClient client;
  private int promiseIndex;

  public EnvironmentConfigurationTest() {
    this.callbackMessages =
      Stream.generate(Promise::<Map.Entry<String, String>>promise)
            .limit(2)
            .collect(Collectors.toList());
  }

  @BeforeEach
  public void setUp(final Vertx vertx, final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    this.client = WebClient.create(vertx);
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
    vertx
        .deployVerticle(
          new MainVerticle(),
          new DeploymentOptions().setConfig((JsonObject) Json.decodeValue(Files.readString(
            Path.of(ClassLoader.getSystemResource("cartago_config.json").toURI()),
            StandardCharsets.UTF_8
          )))
        )
        .compose(r -> vertx.deployVerticle(new CallbackServerVerticle()))
        .onComplete(ctx.succeedingThenComplete());
  }

  @AfterEach
  public void tearDown(final Vertx vertx, final VertxTestContext ctx) {
    vertx.close().onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testRun(final VertxTestContext ctx) throws URISyntaxException, IOException {
    final var workspaceRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("test_workspace_sub_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    final var artifactRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("c0_counter_artifact_sub_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    final var subWorkspaceRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("sub_workspace_c0_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    this.client
        .get(TEST_PORT, TEST_HOST, WORKSPACES_PATH + MAIN_WORKSPACE_NAME)
        .send()
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              OK_STATUS_MESSAGE
          );
          this.assertEqualsThingDescriptions(
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
          this.assertEqualsThingDescriptions(
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
          this.assertEqualsThingDescriptions(
              artifactRepresentation,
              r.bodyAsString()
          );
        })
        .compose(r -> this.callbackMessages.get(0).future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              "http://" + TEST_HOST + ":" + TEST_PORT + "/workspaces/sub/artifacts/c0",
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
        .compose(r -> this.callbackMessages.get(1).future())
        .onSuccess(m -> {
          Assertions.assertEquals(
              "http://" + TEST_HOST + ":" + TEST_PORT + "/workspaces/sub/artifacts/c0",
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

  private void assertEqualsThingDescriptions(final String expected, final String actual) {
    Assertions.assertTrue(
        Models.isomorphic(
          TDGraphReader.readFromString(ThingDescription.TDFormat.RDF_TURTLE, expected)
                       .getGraph()
                       .orElseThrow(),
          TDGraphReader.readFromString(ThingDescription.TDFormat.RDF_TURTLE, actual)
                       .getGraph()
                       .orElseThrow()
        ),
        REPRESENTATIONS_EQUAL_MESSAGE
    );
  }
}
