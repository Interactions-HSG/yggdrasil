package org.hyperagents.yggdrasil.store;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.net.URISyntaxException;
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

@ExtendWith(VertxExtension.class)
public class RdfStoreVerticleCreateTest {
  private static final String URIS_EQUAL_MESSAGE = "The URIs should be equal";

  private final BlockingQueue<HttpNotificationDispatcherMessage> notificationQueue;
  private RdfStoreMessagebox storeMessagebox;

  public RdfStoreVerticleCreateTest() {
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
  public void testCreateArtifactMalformedUri(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.CreateArtifact(
          "nonexistent",
          "c0",
          Files.readString(Path.of(
            ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()
          ))
        ))
        .onFailure(RdfStoreVerticleTestHelpers::assertInternalServerError)
        .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testCreateWorkspaceMalformedUri(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.CreateWorkspace(
          "nonexistent",
          "test",
          Optional.empty(),
          Files.readString(Path.of(
            ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()
          ))
        ))
        .onFailure(RdfStoreVerticleTestHelpers::assertInternalServerError)
        .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testCreateAndGetWorkspace(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var platformRepresentation =
          Files.readString(Path.of(
            ClassLoader.getSystemResource("platform_test_td.ttl").toURI()
          ));
      final var outputWorkspaceRepresentation =
          Files.readString(Path.of(
            ClassLoader.getSystemResource("output_test_workspace_td.ttl").toURI()
          ));
    this.assertWorkspaceCreated(ctx, outputWorkspaceRepresentation, platformRepresentation)
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          "http://localhost:8080/"
        )))
        .onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
          platformRepresentation,
          r.body()
        ))
        .compose(r -> this.storeMessagebox.sendMessage(new RdfStoreMessage.GetEntity(
          "http://localhost:8080/workspaces/test"
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
        Files.readString(Path.of(
          ClassLoader.getSystemResource("sub_workspace_td.ttl").toURI()
        ));
    final var outputWorkspaceRepresentation =
        Files.readString(Path.of(
          ClassLoader.getSystemResource("output_sub_workspace_td.ttl").toURI()
        ));
    final var outputParentWorkspaceRepresentation =
        Files.readString(Path.of(
          ClassLoader.getSystemResource("test_workspace_sub_td.ttl").toURI()
        ));
    this.assertWorkspaceCreated(
      ctx,
      Files.readString(Path.of(
          ClassLoader.getSystemResource("output_test_workspace_td.ttl").toURI()
      )),
      Files.readString(Path.of(
          ClassLoader.getSystemResource("platform_test_td.ttl").toURI()
      ))
    )
    .compose(r ->  this.storeMessagebox
                       .sendMessage(new RdfStoreMessage.CreateWorkspace(
                         "http://localhost:8080/workspaces/",
                         "sub",
                         Optional.of("http://localhost:8080/workspaces/test"),
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
          "http://localhost:8080/workspaces/test",
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
          "http://localhost:8080/workspaces/",
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
      "http://localhost:8080/workspaces/test"
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
        Files.readString(Path.of(
          ClassLoader.getSystemResource("c0_counter_artifact_td.ttl").toURI()
        ));
    final var outputArtifactRepresentation =
        Files.readString(Path.of(
          ClassLoader.getSystemResource("output_c0_counter_artifact_td.ttl").toURI()
        ));
      final var outputParentWorkspaceRepresentation =
        Files.readString(Path.of(
          ClassLoader.getSystemResource("test_workspace_c0_td.ttl").toURI()
        ));
    this.assertWorkspaceCreated(
      ctx,
      Files.readString(Path.of(
          ClassLoader.getSystemResource("output_test_workspace_td.ttl").toURI()
      )),
      Files.readString(Path.of(
          ClassLoader.getSystemResource("platform_test_td.ttl").toURI()
      ))
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
          "http://localhost:8080/workspaces/test",
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
      "http://localhost:8080/workspaces/test"
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

  private Future<Message<String>> assertWorkspaceCreated(
    final VertxTestContext ctx,
    final String outputWorkspaceRepresentation,
    final String platformRepresentation
  ) throws URISyntaxException, IOException {
    final var inputWorkspaceRepresentation =
        Files.readString(Path.of(
          ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()
        ));
    return this.storeMessagebox
               .sendMessage(new RdfStoreMessage.CreateWorkspace(
                 "http://localhost:8080/workspaces/",
                 "test",
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
                     (HttpNotificationDispatcherMessage.EntityChanged) this.notificationQueue.take();
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
                     (HttpNotificationDispatcherMessage.EntityCreated) this.notificationQueue.take();
                   Assertions.assertEquals(
                     "http://localhost:8080/workspaces/",
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
