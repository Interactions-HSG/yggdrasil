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
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.JUnitAssertionsShouldIncludeMessage"})
@ExtendWith(VertxExtension.class)
public class RdfStoreVerticleDeleteTest {
  private static final String URIS_EQUAL_MESSAGE = "The URIs should be equal";
  private static final String PLATFORM_URI = "http://localhost:8080/";
  private static final String TEST_WORKSPACE_URI = PLATFORM_URI + "workspaces/test";
  private static final String TEST_AGENT_BODY_URI = TEST_WORKSPACE_URI + "/artifacts/body_kai";
  private static final String SUB_WORKSPACE_URI = PLATFORM_URI + "workspaces/sub";
  private static final String COUNTER_ARTIFACT_URI = SUB_WORKSPACE_URI + "/artifacts/c0";
  private static final String COUNTER_ARTIFACT_FILE = "c0_counter_artifact_sub_td.ttl";

  private final BlockingQueue<HttpNotificationDispatcherMessage> notificationQueue;
  private RdfStoreMessagebox storeMessagebox;

  public RdfStoreVerticleDeleteTest() {
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
  public void testDeleteMissingEntity(final VertxTestContext ctx) {
    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.DeleteEntity("http://yggdrasil:8080/"))
        .onFailure(RdfStoreVerticleTestHelpers::assertNotFound)
        .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testMalformedUri(final VertxTestContext ctx) {
    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.DeleteEntity("nonexistent"))
        .onFailure(RdfStoreVerticleTestHelpers::assertBadRequest)
        .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testDeleteAndGetWorkspace(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var deletedWorkspaceDescription =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("test_workspace_sub_body_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var platformDescription =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("platform_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    this.assertWorkspaceTreeCreated(ctx)
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.DeleteEntity(
            TEST_WORKSPACE_URI + "/"
        )))
        .onSuccess(r -> {
          RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
              deletedWorkspaceDescription,
              r.body()
          );
          try {
            final var platformUpdateMessage =
                (HttpNotificationDispatcherMessage.EntityChanged) this.notificationQueue.take();
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                platformDescription,
                platformUpdateMessage.content()
            );
            Assertions.assertEquals(
                PLATFORM_URI,
                platformUpdateMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );

            final var changedMessage =
                (HttpNotificationDispatcherMessage.EntityChanged) this.notificationQueue.take();

            Assertions.assertEquals(
                PLATFORM_URI + "workspaces/",
                changedMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );

            final var expected = "@base<http://localhost:8080/>."
                + "@prefixhmas:<https://purl.org/hmas/>."
                + "<#platform>ahmas:HypermediaMASPlatform.";

            Assertions.assertEquals(
                expected,
                removeWhitespace(changedMessage.content()),
                URIS_EQUAL_MESSAGE
            );

            final var deletionMessage =
                (HttpNotificationDispatcherMessage.EntityDeleted) this.notificationQueue.take();
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                deletedWorkspaceDescription,
                deletionMessage.content()
            );

            Assertions.assertEquals(
                TEST_WORKSPACE_URI + "/",
                deletionMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );

            final var bodyDeletionMessage =
                (HttpNotificationDispatcherMessage.EntityDeleted) this.notificationQueue.take();
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                Files.readString(
                    Path.of(
                        ClassLoader.getSystemResource("output_test_agent_body_test.ttl").toURI()),
                    StandardCharsets.UTF_8
                ),
                bodyDeletionMessage.content()
            );
            Assertions.assertEquals(
                TEST_AGENT_BODY_URI + "/",
                bodyDeletionMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );
            final var subWorkspaceDeletionMessage =
                (HttpNotificationDispatcherMessage.EntityDeleted) this.notificationQueue.take();
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                Files.readString(
                    Path.of(ClassLoader.getSystemResource("sub_workspace_c0_td.ttl").toURI()),
                    StandardCharsets.UTF_8
                ),
                subWorkspaceDeletionMessage.content()
            );
            Assertions.assertEquals(
                SUB_WORKSPACE_URI + "/",
                subWorkspaceDeletionMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );
            final var artifactDeletionMessage =
                (HttpNotificationDispatcherMessage.EntityDeleted) this.notificationQueue.take();
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                Files.readString(
                    Path.of(ClassLoader.getSystemResource(COUNTER_ARTIFACT_FILE).toURI()),
                    StandardCharsets.UTF_8
                ),
                artifactDeletionMessage.content()
            );
            Assertions.assertEquals(
                COUNTER_ARTIFACT_URI + "/",
                artifactDeletionMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );
          } catch (final Exception e) {
            ctx.failNow(e);
          }
        })
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
            TEST_WORKSPACE_URI
        )))
        .onFailure(RdfStoreVerticleTestHelpers::assertNotFound)
        .recover(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
            SUB_WORKSPACE_URI
        )))
        .onFailure(RdfStoreVerticleTestHelpers::assertNotFound)
        .recover(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
            COUNTER_ARTIFACT_URI
        )))
        .onFailure(RdfStoreVerticleTestHelpers::assertNotFound)
        .recover(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
            PLATFORM_URI
        )))
        .onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
            platformDescription,
            r.body()
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testDeleteAndGetSubWorkspace(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var deletedWorkspaceDescription =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("sub_workspace_c0_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var parentWorkspaceDescription =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("test_workspace_body_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var platformDescription =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("platform_test_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    this.assertWorkspaceTreeCreated(ctx)
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.DeleteEntity(
            SUB_WORKSPACE_URI + "/"
        )))
        .onSuccess(r -> {
          RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
              deletedWorkspaceDescription,
              r.body()
          );
          try {
            final var parentWorkspaceUpdateMessage =
                (HttpNotificationDispatcherMessage.EntityChanged) this.notificationQueue.take();
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                parentWorkspaceDescription,
                parentWorkspaceUpdateMessage.content()
            );
            Assertions.assertEquals(
                TEST_WORKSPACE_URI + "/",
                parentWorkspaceUpdateMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );

            final var changedMessage =
                (HttpNotificationDispatcherMessage.EntityChanged) this.notificationQueue.take();

            Assertions.assertEquals(
                PLATFORM_URI + "workspaces?parent=test",
                changedMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );

            final var expected = "@base<http://localhost:8080/>."
                + "@prefixhmas:<https://purl.org/hmas/>."
                + "<workspaces/test/#workspace>ahmas:Workspace.";

            Assertions.assertEquals(
                expected,
                removeWhitespace(changedMessage.content()),
                URIS_EQUAL_MESSAGE
            );


            final var deletionMessage =
                (HttpNotificationDispatcherMessage.EntityDeleted) this.notificationQueue.take();
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                deletedWorkspaceDescription,
                deletionMessage.content()
            );
            Assertions.assertEquals(
                SUB_WORKSPACE_URI + "/",
                deletionMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );
            final var artifactDeletionMessage =
                (HttpNotificationDispatcherMessage.EntityDeleted) this.notificationQueue.take();
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                Files.readString(
                    Path.of(ClassLoader.getSystemResource(COUNTER_ARTIFACT_FILE).toURI()),
                    StandardCharsets.UTF_8
                ),
                artifactDeletionMessage.content()
            );
            Assertions.assertEquals(
                COUNTER_ARTIFACT_URI + "/",
                artifactDeletionMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );
          } catch (final Exception e) {
            ctx.failNow(e);
          }
        })
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
            SUB_WORKSPACE_URI
        )))
        .onFailure(RdfStoreVerticleTestHelpers::assertNotFound)
        .recover(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
            COUNTER_ARTIFACT_URI
        )))
        .onFailure(RdfStoreVerticleTestHelpers::assertNotFound)
        .recover(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
            TEST_WORKSPACE_URI
        )))
        .onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
            parentWorkspaceDescription,
            r.body()
        ))
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
            PLATFORM_URI
        )))
        .onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
            platformDescription,
            r.body()
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testDeleteAndGetArtifact(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var deletedArtifactDescription =
        Files.readString(
            Path.of(ClassLoader.getSystemResource(COUNTER_ARTIFACT_FILE).toURI()),
            StandardCharsets.UTF_8
        );
    final var workspaceDescription =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("output_sub_workspace_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var parentWorkspaceDescription =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("test_workspace_sub_body_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var platformDescription =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("platform_test_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    this.assertWorkspaceTreeCreated(ctx)
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.DeleteEntity(
            COUNTER_ARTIFACT_URI + "/"
        )))
        .onSuccess(r -> {
          RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
              deletedArtifactDescription,
              r.body()
          );
          try {
            final var parentWorkspaceUpdateMessage =
                (HttpNotificationDispatcherMessage.EntityChanged) this.notificationQueue.take();
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                Files.readString(
                    Path.of(ClassLoader.getSystemResource("output_sub_workspace_td.ttl").toURI()),
                    StandardCharsets.UTF_8
                ),
                parentWorkspaceUpdateMessage.content()
            );
            Assertions.assertEquals(
                SUB_WORKSPACE_URI,
                parentWorkspaceUpdateMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );
            final var deletionMessage =
                (HttpNotificationDispatcherMessage.EntityDeleted) this.notificationQueue.take();
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                Files.readString(
                    Path.of(ClassLoader.getSystemResource(COUNTER_ARTIFACT_FILE).toURI()),
                    StandardCharsets.UTF_8
                ),
                deletionMessage.content()
            );
            Assertions.assertEquals(
                COUNTER_ARTIFACT_URI,
                deletionMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );
          } catch (final Exception e) {
            ctx.failNow(e);
          }
        })
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
            COUNTER_ARTIFACT_URI
        )))
        .onFailure(RdfStoreVerticleTestHelpers::assertNotFound)
        .recover(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
            SUB_WORKSPACE_URI
        )))
        .onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
            workspaceDescription,
            r.body()
        ))
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
            TEST_WORKSPACE_URI
        )))
        .onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
            parentWorkspaceDescription,
            r.body()
        ))
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
            PLATFORM_URI
        )))
        .onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
            platformDescription,
            r.body()
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testDeleteAndGetBody(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var deletedBodyDescription =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("output_test_agent_body_test.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var workspaceDescription =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("test_workspace_sub_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var platformDescription =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("platform_test_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    this.assertWorkspaceTreeCreated(ctx)
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.DeleteEntity(
            TEST_AGENT_BODY_URI + "/"
        )))
        .onSuccess(r -> {
          RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
              deletedBodyDescription,
              r.body()
          );
          try {
            final var parentWorkspaceUpdateMessage =
                (HttpNotificationDispatcherMessage.EntityChanged) this.notificationQueue.take();
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                workspaceDescription,
                parentWorkspaceUpdateMessage.content()
            );
            Assertions.assertEquals(
                TEST_WORKSPACE_URI,
                parentWorkspaceUpdateMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );
            final var deletionMessage =
                (HttpNotificationDispatcherMessage.EntityDeleted) this.notificationQueue.take();
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                deletedBodyDescription,
                deletionMessage.content()
            );
            Assertions.assertEquals(
                TEST_AGENT_BODY_URI,
                deletionMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );
          } catch (final Exception e) {
            ctx.failNow(e);
          }
        })
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
            TEST_AGENT_BODY_URI
        )))
        .onFailure(RdfStoreVerticleTestHelpers::assertNotFound)
        .recover(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
            TEST_WORKSPACE_URI
        )))
        .onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
            workspaceDescription,
            r.body()
        ))
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
            PLATFORM_URI
        )))
        .onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
            platformDescription,
            r.body()
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  private Future<Message<String>> assertWorkspaceTreeCreated(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var inputWorkspaceRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var inputSubWorkspaceRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("sub_workspace_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var inputArtifactRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource(COUNTER_ARTIFACT_FILE).toURI()),
            StandardCharsets.UTF_8
        );
    final var inputBodyRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("test_agent_body_test.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    return this.storeMessagebox
        .sendMessage(new RdfStoreMessage.CreateWorkspace(
            "http://localhost:8080/workspaces/",
            "test",
            Optional.empty(),
            inputWorkspaceRepresentation
        )).onSuccess(r -> {
          try {
            this.notificationQueue.take();
            this.notificationQueue.take();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        })
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.CreateWorkspace(
            "http://localhost:8080/workspaces/",
            "sub",
            Optional.of(TEST_WORKSPACE_URI),
            inputSubWorkspaceRepresentation
        ))).onSuccess(r -> {
          try {
            this.notificationQueue.take();
            this.notificationQueue.take();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        })
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.CreateArtifact(
            "http://localhost:8080/workspaces/sub/artifacts/",
            "c0",
            inputArtifactRepresentation
        ))).onSuccess(r -> {
          try {
            this.notificationQueue.take();
            this.notificationQueue.take();
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        })
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.CreateBody(
            "test",
            "http://localhost:8080/agent/kai",
            "kai",
            inputBodyRepresentation
        )))
        .onSuccess(r -> {
          try {
            this.notificationQueue.take();
            this.notificationQueue.take();
            this.notificationQueue.take();
          } catch (final Exception e) {
            ctx.failNow(e);
          }
        });
  }

  private String removeWhitespace(final String input) {
    return input.replaceAll("\\s+", "");
  }
}
