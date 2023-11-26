package org.hyperagents.yggdrasil.store;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@ExtendWith(VertxExtension.class)
public class RdfStoreVerticleDeleteTest {
  private static final String URIS_EQUAL_MESSAGE = "The URIs should be equal";

  private final BlockingQueue<HttpNotificationDispatcherMessage> notificationQueue;
  private RdfStoreMessagebox storeMessagebox;

  public RdfStoreVerticleDeleteTest() {
    this.notificationQueue = new LinkedBlockingQueue<>();
  }

  @BeforeEach
  public void setUp(final Vertx vertx, final VertxTestContext ctx) {
    this.storeMessagebox = new RdfStoreMessagebox(vertx.eventBus());
    final var notificationMessagebox = new HttpNotificationDispatcherMessagebox(vertx.eventBus());
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
        .onFailure(RdfStoreVerticleTestHelpers::assertInternalServerError)
        .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testDeleteAndGetWorkspace(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var deletedWorkspaceDescription =
        Files.readString(Path.of(
          ClassLoader.getSystemResource("test_workspace_sub_td.ttl").toURI()
        ));
    final var platformDescription =
        Files.readString(Path.of(
          ClassLoader.getSystemResource("platform_td.ttl").toURI()
        ));
    this.assertWorkspaceTreeCreated(ctx)
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.DeleteEntity(
          "http://localhost:8080/workspaces/test"
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
              "http://localhost:8080/",
              platformUpdateMessage.requestIri(),
              URIS_EQUAL_MESSAGE
            );
            final var deletionMessage =
              (HttpNotificationDispatcherMessage.EntityDeleted) this.notificationQueue.take();
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
              deletedWorkspaceDescription,
              deletionMessage.content()
            );
            Assertions.assertEquals(
              "http://localhost:8080/workspaces/test",
              deletionMessage.requestIri(),
              URIS_EQUAL_MESSAGE
            );
            final var subWorkspaceDeletionMessage =
              (HttpNotificationDispatcherMessage.EntityDeleted) this.notificationQueue.take();
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
              Files.readString(Path.of(
                ClassLoader.getSystemResource("sub_workspace_c0_td.ttl").toURI()
              )),
              subWorkspaceDeletionMessage.content()
            );
            Assertions.assertEquals(
              "http://localhost:8080/workspaces/sub",
              subWorkspaceDeletionMessage.requestIri(),
              URIS_EQUAL_MESSAGE
            );
            final var artifactDeletionMessage =
              (HttpNotificationDispatcherMessage.EntityDeleted) this.notificationQueue.take();
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
              Files.readString(Path.of(
                ClassLoader.getSystemResource("c0_counter_artifact_sub_td.ttl").toURI()
              )),
              artifactDeletionMessage.content()
            );
            Assertions.assertEquals(
              "http://localhost:8080/workspaces/sub/artifacts/c0",
              artifactDeletionMessage.requestIri(),
              URIS_EQUAL_MESSAGE
            );
          } catch (final Exception e) {
            ctx.failNow(e);
          }
        })
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          "http://localhost:8080/workspaces/test"
        )))
        .onFailure(RdfStoreVerticleTestHelpers::assertNotFound)
        .recover(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          "http://localhost:8080/workspaces/sub"
        )))
        .onFailure(RdfStoreVerticleTestHelpers::assertNotFound)
        .recover(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          "http://localhost:8080/workspaces/sub/artifacts/c0"
        )))
        .onFailure(RdfStoreVerticleTestHelpers::assertNotFound)
        .recover(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          "http://localhost:8080/"
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
        Files.readString(Path.of(
          ClassLoader.getSystemResource("sub_workspace_c0_td.ttl").toURI()
        ));
    final var parentWorkspaceDescription =
        Files.readString(Path.of(
          ClassLoader.getSystemResource("output_test_workspace_td.ttl").toURI()
        ));
    final var platformDescription =
        Files.readString(Path.of(
          ClassLoader.getSystemResource("platform_test_td.ttl").toURI()
        ));
    this.assertWorkspaceTreeCreated(ctx)
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.DeleteEntity(
          "http://localhost:8080/workspaces/sub"
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
              Files.readString(Path.of(
                ClassLoader.getSystemResource("output_test_workspace_td.ttl").toURI()
              )),
              parentWorkspaceUpdateMessage.content()
            );
            Assertions.assertEquals(
              "http://localhost:8080/workspaces/test",
              parentWorkspaceUpdateMessage.requestIri(),
              URIS_EQUAL_MESSAGE
            );
            final var deletionMessage =
              (HttpNotificationDispatcherMessage.EntityDeleted) this.notificationQueue.take();
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
              Files.readString(Path.of(
                ClassLoader.getSystemResource("sub_workspace_c0_td.ttl").toURI()
              )),
              deletionMessage.content()
            );
            Assertions.assertEquals(
              "http://localhost:8080/workspaces/sub",
              deletionMessage.requestIri(),
              URIS_EQUAL_MESSAGE
            );
            final var artifactDeletionMessage =
              (HttpNotificationDispatcherMessage.EntityDeleted) this.notificationQueue.take();
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
              Files.readString(Path.of(
                ClassLoader.getSystemResource("c0_counter_artifact_sub_td.ttl").toURI()
              )),
              artifactDeletionMessage.content()
            );
            Assertions.assertEquals(
              "http://localhost:8080/workspaces/sub/artifacts/c0",
              artifactDeletionMessage.requestIri(),
              URIS_EQUAL_MESSAGE
            );
          } catch (final Exception e) {
            ctx.failNow(e);
          }
        })
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          "http://localhost:8080/workspaces/sub"
        )))
        .onFailure(RdfStoreVerticleTestHelpers::assertNotFound)
        .recover(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          "http://localhost:8080/workspaces/sub/artifacts/c0"
        )))
        .onFailure(RdfStoreVerticleTestHelpers::assertNotFound)
        .recover(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          "http://localhost:8080/workspaces/test"
        )))
        .onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
          parentWorkspaceDescription,
          r.body()
        ))
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          "http://localhost:8080/"
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
        Files.readString(Path.of(
          ClassLoader.getSystemResource("c0_counter_artifact_sub_td.ttl").toURI()
        ));
    final var workspaceDescription =
        Files.readString(Path.of(
          ClassLoader.getSystemResource("output_sub_workspace_td.ttl").toURI()
        ));
    final var parentWorkspaceDescription =
        Files.readString(Path.of(
          ClassLoader.getSystemResource("test_workspace_sub_td.ttl").toURI()
        ));
    final var platformDescription =
        Files.readString(Path.of(
          ClassLoader.getSystemResource("platform_test_td.ttl").toURI()
        ));
    this.assertWorkspaceTreeCreated(ctx)
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.DeleteEntity(
          "http://localhost:8080/workspaces/sub/artifacts/c0"
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
              Files.readString(Path.of(
                ClassLoader.getSystemResource("output_sub_workspace_td.ttl").toURI()
              )),
              parentWorkspaceUpdateMessage.content()
            );
            Assertions.assertEquals(
              "http://localhost:8080/workspaces/sub",
              parentWorkspaceUpdateMessage.requestIri(),
              URIS_EQUAL_MESSAGE
            );
            final var deletionMessage =
              (HttpNotificationDispatcherMessage.EntityDeleted) this.notificationQueue.take();
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
              Files.readString(Path.of(
                ClassLoader.getSystemResource("c0_counter_artifact_sub_td.ttl").toURI()
              )),
              deletionMessage.content()
            );
            Assertions.assertEquals(
              "http://localhost:8080/workspaces/sub/artifacts/c0",
              deletionMessage.requestIri(),
              URIS_EQUAL_MESSAGE
            );
          } catch (final Exception e) {
            ctx.failNow(e);
          }
        })
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          "http://localhost:8080/workspaces/sub/artifacts/c0"
        )))
        .onFailure(RdfStoreVerticleTestHelpers::assertNotFound)
        .recover(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          "http://localhost:8080/workspaces/sub"
        )))
        .onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
          workspaceDescription,
          r.body()
        ))
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          "http://localhost:8080/workspaces/test"
        )))
        .onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
          parentWorkspaceDescription,
          r.body()
        ))
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          "http://localhost:8080/"
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
        Files.readString(Path.of(
          ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()
        ));
    final var inputSubWorkspaceRepresentation =
        Files.readString(Path.of(
          ClassLoader.getSystemResource("sub_workspace_td.ttl").toURI()
        ));
    final var inputArtifactRepresentation =
        Files.readString(Path.of(
          ClassLoader.getSystemResource("c0_counter_artifact_sub_td.ttl").toURI()
        ));
    return this.storeMessagebox
               .sendMessage(new RdfStoreMessage.CreateWorkspace(
                 "http://localhost:8080/workspaces/",
                 "test",
                 Optional.empty(),
                 inputWorkspaceRepresentation
               ))
               .compose(r -> {
                 try {
                   this.notificationQueue.take();
                   this.notificationQueue.take();
                 } catch (final Exception e) {
                   ctx.failNow(e);
                 }
                 return this.storeMessagebox.sendMessage(new RdfStoreMessage.CreateWorkspace(
                   "http://localhost:8080/workspaces/",
                   "sub",
                   Optional.of("http://localhost:8080/workspaces/test"),
                   inputSubWorkspaceRepresentation
                 ));
               })
               .compose(r -> {
                 try {
                   this.notificationQueue.take();
                   this.notificationQueue.take();
                 } catch (final Exception e) {
                   ctx.failNow(e);
                 }
                 return this.storeMessagebox.sendMessage(new RdfStoreMessage.CreateArtifact(
                   "http://localhost:8080/workspaces/sub/artifacts/",
                   "c0",
                   inputArtifactRepresentation
                 ));
               })
               .onSuccess(r -> {
                 try {
                   this.notificationQueue.take();
                   this.notificationQueue.take();
                 } catch (final Exception e) {
                   ctx.failNow(e);
                 }
               });
  }
}
