package org.hyperagents.yggdrasil.store;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
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
 * testclass.
 */
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@ExtendWith(VertxExtension.class)
public class RdfStoreVerticleCreateTest {
  private static final String URIS_EQUAL_MESSAGE = "The URIs should be equal";
  private static final String WORKSPACES_PATH = "http://localhost:8080/workspaces/";
  private static final String TEST_WORKSPACE_NAME = "test";
  private static final String PLATFORM_FILE = "platform_test_td.ttl";
  private static final String TEST_WORKSPACE_FILE = "output_test_workspace_td.ttl";

  private final BlockingQueue<HttpNotificationDispatcherMessage> notificationQueue;
  private RdfStoreMessagebox storeMessagebox;

  public RdfStoreVerticleCreateTest() {
    this.notificationQueue = new LinkedBlockingQueue<>();
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
         .put("default", httpConfig);
    final var notificationConfig = new WebSubConfigImpl(
        JsonObject.of(
          "notification-config",
          JsonObject.of("enabled", true)
        ),
        httpConfig
    );
    vertx.sharedData()
         .getLocalMap("environment-config")
         .put("default",
              new EnvironmentConfigImpl(JsonObject.of(
                "environment-config",
                JsonObject.of(
                  "enabled",
                  true,
                  "ontology",
                  "td"
                )
              )));
    vertx.sharedData()
         .getLocalMap("notification-config")
         .put("default", notificationConfig);
    this.storeMessagebox = new RdfStoreMessagebox(vertx.eventBus());
    final var notificationMessagebox = new HttpNotificationDispatcherMessagebox(
        vertx.eventBus(),
        notificationConfig
    );
    notificationMessagebox.init();
    notificationMessagebox.receiveMessages(m -> this.notificationQueue.add(m.body()));
    vertx.deployVerticle(new RdfStoreVerticle(), ctx.succeedingThenComplete());
  }

  @AfterEach
  public void tearDown(final Vertx vertx, final VertxTestContext ctx) {
    vertx.close(ctx.succeedingThenComplete());
  }

  @Test
  public void testCreateArtifactMalformedUri(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.CreateArtifact(
          "nonexistent",
          "c0",
          Files.readString(
            Path.of(ClassLoader.getSystemResource("c0_counter_artifact_td.ttl").toURI()),
            StandardCharsets.UTF_8
          )
        ))
        .onFailure(RdfStoreVerticleTestHelpers::assertBadRequest)
        .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testCreateWorkspaceMalformedUri(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.CreateWorkspace(
          "nonexistent",
          TEST_WORKSPACE_NAME,
          Optional.empty(),
          Files.readString(
            Path.of(ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()),
            StandardCharsets.UTF_8
          )
        ))
        .onFailure(RdfStoreVerticleTestHelpers::assertBadRequest)
        .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testCreateAndGetWorkspace(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var platformRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource(PLATFORM_FILE).toURI()),
          StandardCharsets.UTF_8
        );
    final var outputWorkspaceRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource(TEST_WORKSPACE_FILE).toURI()),
          StandardCharsets.UTF_8
        );
    this.assertWorkspaceCreated(ctx, outputWorkspaceRepresentation, platformRepresentation)
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          "http://localhost:8080/"
        )))
        .onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
          platformRepresentation,
          r.body()
        ))
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          WORKSPACES_PATH + TEST_WORKSPACE_NAME
        )))
        .onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
          outputWorkspaceRepresentation,
          r.body()
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testCreateAndGetSubWorkspace(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var inputWorkspaceRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("sub_workspace_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    final var outputWorkspaceRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("output_sub_workspace_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    final var outputParentWorkspaceRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("test_workspace_sub_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    this.assertWorkspaceCreated(
          ctx,
          Files.readString(
              Path.of(ClassLoader.getSystemResource(TEST_WORKSPACE_FILE).toURI()),
              StandardCharsets.UTF_8
          ),
          Files.readString(
              Path.of(ClassLoader.getSystemResource(PLATFORM_FILE).toURI()),
              StandardCharsets.UTF_8
          )
        )
        .compose(r ->  this.storeMessagebox
                           .sendMessage(new RdfStoreMessage.CreateWorkspace(
                             WORKSPACES_PATH,
                             "sub",
                             Optional.of(WORKSPACES_PATH + TEST_WORKSPACE_NAME),
                             inputWorkspaceRepresentation
                           ))
        )
        .onSuccess(r -> {
          try {
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                outputWorkspaceRepresentation,
                r.body()
            );
            final var entityUpdatedMessage =
                (HttpNotificationDispatcherMessage.EntityChanged) this.notificationQueue.take();
            Assertions.assertEquals(
                WORKSPACES_PATH + "test",
                entityUpdatedMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                outputParentWorkspaceRepresentation,
                entityUpdatedMessage.content()
            );
            final var entityCreatedMessage =
                (HttpNotificationDispatcherMessage.EntityCreated) this.notificationQueue.take();
            Assertions.assertEquals(
                WORKSPACES_PATH,
                entityCreatedMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                outputWorkspaceRepresentation,
                entityCreatedMessage.content()
            );
          } catch (final Exception e) {
            ctx.failNow(e);
          }
        })
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          WORKSPACES_PATH + TEST_WORKSPACE_NAME
        )))
        .onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
          outputParentWorkspaceRepresentation,
          r.body()
        ))
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          "http://localhost:8080/workspaces/sub"
        )))
        .onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
          outputWorkspaceRepresentation,
          r.body()
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testCreateAndGetArtifact(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var artifactRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("c0_counter_artifact_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var outputArtifactRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("output_c0_counter_artifact_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var outputParentWorkspaceRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("test_workspace_c0_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    this.assertWorkspaceCreated(
          ctx,
          Files.readString(
              Path.of(ClassLoader.getSystemResource(TEST_WORKSPACE_FILE).toURI()),
              StandardCharsets.UTF_8
          ),
          Files.readString(
              Path.of(ClassLoader.getSystemResource(PLATFORM_FILE).toURI()),
              StandardCharsets.UTF_8
          )
        )
        .compose(r -> this.storeMessagebox
                          .sendMessage(new RdfStoreMessage.CreateArtifact(
                            "http://localhost:8080/workspaces/test/artifacts/",
                            "c0",
                            artifactRepresentation
                          ))
        )
        .onSuccess(r -> {
          try {
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                outputArtifactRepresentation,
                r.body()
            );
            final var entityUpdatedMessage =
                (HttpNotificationDispatcherMessage.EntityChanged) this.notificationQueue.take();
            Assertions.assertEquals(
                WORKSPACES_PATH + TEST_WORKSPACE_NAME,
                entityUpdatedMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                outputParentWorkspaceRepresentation,
                entityUpdatedMessage.content()
            );
            final var entityCreatedMessage =
                (HttpNotificationDispatcherMessage.EntityCreated) this.notificationQueue.take();
            Assertions.assertEquals(
                "http://localhost:8080/workspaces/test/artifacts/",
                entityCreatedMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                outputArtifactRepresentation,
                entityCreatedMessage.content()
            );
          } catch (final Exception e) {
            ctx.failNow(e);
          }
        })
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          WORKSPACES_PATH + TEST_WORKSPACE_NAME
        )))
        .onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
          outputParentWorkspaceRepresentation,
          r.body()
        ))
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          "http://localhost:8080/workspaces/test/artifacts/c0"
        )))
        .onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
          outputArtifactRepresentation,
          r.body()
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testCreateAndGetBody(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var bodyRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("test_agent_body_test.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var bodyArtifactRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("output_test_agent_body_test.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var outputParentWorkspaceRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("test_workspace_body_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    this.assertWorkspaceCreated(
          ctx,
          Files.readString(
              Path.of(ClassLoader.getSystemResource(TEST_WORKSPACE_FILE).toURI()),
              StandardCharsets.UTF_8
          ),
          Files.readString(
              Path.of(ClassLoader.getSystemResource(PLATFORM_FILE).toURI()),
              StandardCharsets.UTF_8
          )
        )
        .compose(r -> this.storeMessagebox
                          .sendMessage(new RdfStoreMessage.CreateBody(
                            TEST_WORKSPACE_NAME,
                            "http://localhost:8080/agent/kai",
                            "kai",
                            bodyRepresentation
                          ))
        )
        .onSuccess(r -> {
          try {
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                bodyArtifactRepresentation,
                r.body()
            );
            final var entityUpdatedMessage =
                (HttpNotificationDispatcherMessage.EntityChanged) this.notificationQueue.take();
            Assertions.assertEquals(
                WORKSPACES_PATH + TEST_WORKSPACE_NAME,
                entityUpdatedMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                outputParentWorkspaceRepresentation,
                entityUpdatedMessage.content()
            );
            final var entityCreatedMessage =
                (HttpNotificationDispatcherMessage.EntityCreated) this.notificationQueue.take();
            Assertions.assertEquals(
                "http://localhost:8080/workspaces/test/artifacts/",
                entityCreatedMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                bodyArtifactRepresentation,
                entityCreatedMessage.content()
            );
          } catch (final Exception e) {
            ctx.failNow(e);
          }
        })
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          WORKSPACES_PATH + TEST_WORKSPACE_NAME
        )))
        .onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
          outputParentWorkspaceRepresentation,
          r.body()
        ))
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          "http://localhost:8080/workspaces/test/artifacts/body_kai/"
        )))
        .onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
          bodyArtifactRepresentation,
          r.body()
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  private Future<Message<String>> assertWorkspaceCreated(
      final VertxTestContext ctx,
      final String outputWorkspaceRepresentation,
      final String platformRepresentation
  ) throws URISyntaxException, IOException {
    final var inputWorkspaceRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    return this.storeMessagebox
               .sendMessage(new RdfStoreMessage.CreateWorkspace(
                 WORKSPACES_PATH,
                 TEST_WORKSPACE_NAME,
                 Optional.empty(),
                 inputWorkspaceRepresentation
               ))
               .onSuccess(r -> {
                 try {
                   RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                       outputWorkspaceRepresentation,
                       r.body()
                   );
                   final var entityUpdatedMessage =
                       (HttpNotificationDispatcherMessage.EntityChanged)
                         this.notificationQueue.take();
                   Assertions.assertEquals(
                       "http://localhost:8080/",
                       entityUpdatedMessage.requestIri(),
                       URIS_EQUAL_MESSAGE
                   );
                   RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                       platformRepresentation,
                       entityUpdatedMessage.content()
                   );
                   final var entityCreatedMessage =
                       (HttpNotificationDispatcherMessage.EntityCreated)
                         this.notificationQueue.take();
                   Assertions.assertEquals(
                       WORKSPACES_PATH,
                       entityCreatedMessage.requestIri(),
                       URIS_EQUAL_MESSAGE
                   );
                   RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                       outputWorkspaceRepresentation,
                       entityCreatedMessage.content()
                   );
                 } catch (final Exception e) {
                   ctx.failNow(e);
                 }
               });
  }
}
