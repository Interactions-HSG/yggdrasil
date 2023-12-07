package org.hyperagents.yggdrasil.store;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@ExtendWith(VertxExtension.class)
public class RdfStoreVerticleUpdateTest {
  private static final String URIS_EQUAL_MESSAGE = "The URIs should be equal";

  private final BlockingQueue<HttpNotificationDispatcherMessage> notificationQueue;
  private RdfStoreMessagebox storeMessagebox;

  public RdfStoreVerticleUpdateTest() {
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
  public void testUpdateMissingEntity(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.UpdateEntity(
          "http://yggdrasil:8080/",
          Files.readString(
            Path.of(ClassLoader.getSystemResource("updated_test_workspace_td.ttl").toURI()),
            StandardCharsets.UTF_8
          )
        ))
        .onFailure(RdfStoreVerticleTestHelpers::assertNotFound)
        .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testMalformedUri(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.UpdateEntity(
          "nonexistent",
          Files.readString(
            Path.of(ClassLoader.getSystemResource("updated_test_workspace_td.ttl").toURI()),
            StandardCharsets.UTF_8
          )
        ))
        .onFailure(RdfStoreVerticleTestHelpers::assertBadRequest)
        .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testUpdateAndGetWorkspace(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var updatedWorkspaceDescription =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("updated_test_workspace_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    this.assertWorkspaceTreeCreated(ctx)
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.UpdateEntity(
          "http://localhost:8080/workspaces/test",
          updatedWorkspaceDescription
        )))
        .onSuccess(r -> {
          RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
              updatedWorkspaceDescription,
              r.body()
          );
          try {
            final var updateMessage =
                (HttpNotificationDispatcherMessage.EntityChanged) this.notificationQueue.take();
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                updatedWorkspaceDescription,
                updateMessage.content()
            );
            Assertions.assertEquals(
                "http://localhost:8080/workspaces/test",
                updateMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );
          } catch (final Exception e) {
            ctx.failNow(e);
          }
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testDeleteAndGetSubWorkspace(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var updatedWorkspaceDescription =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("updated_sub_workspace_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    this.assertWorkspaceTreeCreated(ctx)
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.UpdateEntity(
          "http://localhost:8080/workspaces/sub",
          updatedWorkspaceDescription
        )))
        .onSuccess(r -> {
          RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
              updatedWorkspaceDescription,
              r.body()
          );
          try {
            final var updateMessage =
                (HttpNotificationDispatcherMessage.EntityChanged) this.notificationQueue.take();
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                updatedWorkspaceDescription,
                updateMessage.content()
            );
            Assertions.assertEquals(
                "http://localhost:8080/workspaces/sub",
                updateMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );
          } catch (final Exception e) {
            ctx.failNow(e);
          }
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testDeleteAndGetArtifact(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var updatedArtifactDescription =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("updated_counter_artifact_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    this.assertWorkspaceTreeCreated(ctx)
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.UpdateEntity(
          "http://localhost:8080/workspaces/sub/artifacts/c0",
          updatedArtifactDescription
        )))
        .onSuccess(r -> {
          RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
              updatedArtifactDescription,
              r.body()
          );
          try {
            final var updateMessage =
                (HttpNotificationDispatcherMessage.EntityChanged) this.notificationQueue.take();
            RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
                updatedArtifactDescription,
                updateMessage.content()
            );
            Assertions.assertEquals(
                "http://localhost:8080/workspaces/sub/artifacts/c0",
                updateMessage.requestIri(),
                URIS_EQUAL_MESSAGE
            );
          } catch (final Exception e) {
            ctx.failNow(e);
          }
        })
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
          Path.of(ClassLoader.getSystemResource("c0_counter_artifact_sub_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
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
