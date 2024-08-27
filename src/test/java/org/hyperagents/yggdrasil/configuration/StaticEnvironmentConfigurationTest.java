package org.hyperagents.yggdrasil.configuration;

import io.vertx.core.DeploymentOptions;
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
import org.apache.hc.core5.http.HttpStatus;
import org.hyperagents.yggdrasil.MainVerticle;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hyperagents.yggdrasil.TConstants.*;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@ExtendWith(VertxExtension.class)
public class StaticEnvironmentConfigurationTest {

  private WebClient client;

  @BeforeEach
  public void setUp(final Vertx vertx, final VertxTestContext ctx,final TestInfo testInfo)
      throws URISyntaxException, IOException {
    final String conf;
    final String testName = testInfo.getTestMethod().orElseThrow().getName();
    if (testName.contains("TD")) {
      conf ="td/static_config.json" ;
    } else if (testName.contains("HMAS")) {
      conf = "hmas/static_config.json";
    } else {
      throw new RuntimeException("Test did not speficy ontology");
    }
    this.client = WebClient.create(vertx);
    vertx
        .deployVerticle(
          new MainVerticle(),
          new DeploymentOptions().setConfig((JsonObject) Json.decodeValue(Files.readString(
            Path.of(ClassLoader.getSystemResource(conf).toURI()),
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
          Path.of(ClassLoader.getSystemResource("td/sub_workspace_c0_td.ttl").toURI()),
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
      .onComplete(ctx.succeedingThenComplete());
  }
}
