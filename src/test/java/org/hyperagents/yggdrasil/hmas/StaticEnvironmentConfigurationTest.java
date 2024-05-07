package org.hyperagents.yggdrasil.hmas;

import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphReader;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.hc.core5.http.HttpStatus;
import org.eclipse.rdf4j.model.util.Models;
import org.hyperagents.yggdrasil.MainVerticle;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@ExtendWith(VertxExtension.class)
public class StaticEnvironmentConfigurationTest {
  private static final String MAIN_WORKSPACE_NAME = "test";
  private static final String SUB_WORKSPACE_NAME = "sub";
  private static final String COUNTER_ARTIFACT_NAME = "c0";
  private static final int TEST_PORT = 8080;
  private static final String TEST_HOST = "localhost";
  private static final String OK_STATUS_MESSAGE = "Status code should be OK";
  private static final String REPRESENTATIONS_EQUAL_MESSAGE = "The representations must be equal";
  private static final String WORKSPACES_PATH = "/workspaces/";
  private static final String ARTIFACTS_PATH = "/artifacts/";

  private WebClient client;

  @BeforeEach
  public void setUp(final Vertx vertx, final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    this.client = WebClient.create(vertx);
    vertx
        .deployVerticle(
          new MainVerticle(),
          new DeploymentOptions().setConfig((JsonObject) Json.decodeValue(Files.readString(
            Path.of(ClassLoader.getSystemResource("hmas/static_config.json").toURI()),
            StandardCharsets.UTF_8
          )))
        )
        .onComplete(ctx.succeedingThenComplete());
  }

  @AfterEach
  public void tearDown(final Vertx vertx, final VertxTestContext ctx) {
    vertx.close().onComplete(ctx.succeedingThenComplete());
  }

  @Test
  @Disabled
  public void testRun(final VertxTestContext ctx) throws URISyntaxException, IOException {
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
          Path.of(ClassLoader.getSystemResource("hmas/sub_workspace_c0_hmas.ttl").toURI()),
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
        .onComplete(ctx.succeedingThenComplete());
  }

  private void assertEqualsThingDescriptions(final String expected, final String actual) {
    Assertions.assertTrue(
      Models.isomorphic(
        ResourceProfileGraphReader.getModelFromString(expected),
        ResourceProfileGraphReader.getModelFromString(actual)
      ),
      REPRESENTATIONS_EQUAL_MESSAGE
    );
  }
}
