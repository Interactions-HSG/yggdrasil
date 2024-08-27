package org.hyperagents.yggdrasil.configuration;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.hyperagents.yggdrasil.CallbackServerVerticle;
import org.hyperagents.yggdrasil.MainVerticle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hyperagents.yggdrasil.TestConstants.*;

@ExtendWith(VertxExtension.class)
public class BaseTests {
  private WebClient client;


  public Future<String> setUp(final Vertx vertx, final JsonObject config) {
    this.client = WebClient.create(vertx);
    return vertx.deployVerticle(new CallbackServerVerticle(8081))
      .compose(r -> vertx.deployVerticle(
        new MainVerticle(),
        new DeploymentOptions().setConfig(config)
      ));
  }

  @AfterEach
  public void tearDown(final Vertx vertx, final VertxTestContext ctx) {
    vertx.close().onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testRunWithoutConfig(final Vertx vertx, final VertxTestContext ctx) throws
    URISyntaxException, IOException {
    final var platformRepresentation =
      Files.readString(
        Path.of(ClassLoader.getSystemResource("ConfigurationTests/basePlatformTD.ttl").toURI()),
        StandardCharsets.UTF_8
      );
    JsonObject config = JsonObject.of();
    setUp(vertx, config)
      .onComplete(x ->
        this.client.get(TEST_PORT, TEST_HOST, "").send()
          .onSuccess(
            r -> {
              assertEqualsThingDescriptions(platformRepresentation, r.bodyAsString());
              ctx.completeNow();
            }
          )
          .onFailure(ctx::failNow)
      );
  }

  @Test
  public void testRunWithConfigNoEnvironmentNoNotification(final Vertx vertx, final VertxTestContext ctx) throws URISyntaxException, IOException {
    final var platformRepresentation =
      Files.readString(
        Path.of(ClassLoader.getSystemResource("ConfigurationTests/basePlatformTD.ttl").toURI()),
        StandardCharsets.UTF_8
      );
    JsonObject config = JsonObject.of(
      HTTP_CONFIG,
      httpConfig
    );
    setUp(vertx, config)
      .onComplete(x ->
        this.client.get(TEST_PORT, TEST_HOST, "").send()
          .onSuccess(
            r -> {
              assertEqualsThingDescriptions(platformRepresentation, r.bodyAsString());
              ctx.completeNow();
            }
          )
          .onFailure(ctx::failNow)
      );
  }

  @Test
  public void testRunWithConfigNoEnvironmentWithNotification(final Vertx vertx, final VertxTestContext ctx) throws URISyntaxException, IOException {
    final var platformRepresentation =
      Files.readString(
        Path.of(ClassLoader.getSystemResource("ConfigurationTests/platformWebSubTD.ttl").toURI()),
        StandardCharsets.UTF_8
      );
    JsonObject config = JsonObject.of(
      HTTP_CONFIG,
      httpConfig,
      NOTIFICATION_CONFIG,
      notificationConfig
    );
    setUp(vertx, config)
      .onComplete(x ->
        this.client.get(TEST_PORT, TEST_HOST, "").send()
          .onSuccess(
            r -> {
              assertEqualsThingDescriptions(platformRepresentation, r.bodyAsString());
              ctx.completeNow();
            }
          )
          .onFailure(ctx::failNow)
      );
  }

  @Test
  public void testRunWithConfigWithEnvironmentNoNotification(final Vertx vertx, final VertxTestContext ctx) throws URISyntaxException, IOException {
    final var platformRepresentation =
      Files.readString(
        Path.of(ClassLoader.getSystemResource("ConfigurationTests/basePlatformTD.ttl").toURI()),
        StandardCharsets.UTF_8
      );
    JsonObject config = JsonObject.of(
      HTTP_CONFIG,
      httpConfig,
      ENVIRONMENT_CONFIG,
      TDEnv
    );
    setUp(vertx, config)
      .onComplete(x ->
        this.client.get(TEST_PORT, TEST_HOST, "").send()
          .onSuccess(
            r -> {
              assertEqualsThingDescriptions(platformRepresentation, r.bodyAsString());
              ctx.completeNow();
            }
          )
          .onFailure(ctx::failNow)
      );
  }

  @Test
  public void testRunWithConfigWithEnvironmentWithNotification(final Vertx vertx, final VertxTestContext ctx) throws URISyntaxException, IOException {
    final var platformRepresentation =
      Files.readString(
        Path.of(ClassLoader.getSystemResource("ConfigurationTests/platformWebSubTD.ttl").toURI()),
        StandardCharsets.UTF_8
      );
    JsonObject config = JsonObject.of(
      HTTP_CONFIG,
      httpConfig,
      NOTIFICATION_CONFIG,
      notificationConfig,
      ENVIRONMENT_CONFIG,
      TDEnv
    );
    setUp(vertx, config)
      .onComplete(x ->
        this.client.get(TEST_PORT, TEST_HOST, "").send()
          .onSuccess(
            r -> {
              assertEqualsThingDescriptions(platformRepresentation, r.bodyAsString());
              ctx.completeNow();
            }
          )
          .onFailure(ctx::failNow)
      );
  }

  @Test
  public void testRunWithConfigWithEnvironmentAndAddedMetadata(final Vertx vertx, final VertxTestContext ctx) throws URISyntaxException, IOException {
    JsonObject config = JsonObject.of(
      HTTP_CONFIG,
      httpConfig,
      NOTIFICATION_CONFIG,
      notificationConfig,
      ENVIRONMENT_CONFIG,
      cartagoEnv
    );
    final var platformRepresentation =
      Files.readString(
        Path.of(ClassLoader.getSystemResource("ConfigurationTests/platformWebSubTDWithWorkspace.ttl").toURI()),
        StandardCharsets.UTF_8
      );
    final var workspaceRepresentation =
      Files.readString(
        Path.of(ClassLoader.getSystemResource("ConfigurationTests/w1_withMetadata.ttl").toURI()),
        StandardCharsets.UTF_8
      );
    final var artifactRepresentation =
      Files.readString(
        Path.of(ClassLoader.getSystemResource("ConfigurationTests/c1_withMetadata.ttl").toURI()),
        StandardCharsets.UTF_8
      );
    setUp(vertx, config).onComplete(x -> this.client.get(TEST_PORT, TEST_HOST, "").send()
      .onSuccess(
        r -> {
          assertEqualsThingDescriptions(platformRepresentation, r.bodyAsString());
          this.client.get(TEST_PORT, TEST_HOST, "/workspaces/w1").send().onSuccess(
            rs -> {
              assertEqualsThingDescriptions(workspaceRepresentation, rs.bodyAsString());
              this.client.get(TEST_PORT,TEST_HOST,"/workspaces/w1/artifacts/c1").send().onSuccess(
                rx -> {
                  assertEqualsThingDescriptions(artifactRepresentation,rx.bodyAsString());
                  ctx.completeNow();
                }
              ).onFailure(ctx::failNow);
            }
          ).onFailure(ctx::failNow);
        }
      )
      .onFailure(ctx::failNow)
    );

  }
}
