package org.hyperagents.yggdrasil.store;

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
import org.hyperagents.yggdrasil.utils.WebSubConfig;
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
public class RdfStoreVerticleGetTest {
  private RdfStoreMessagebox storeMessagebox;
  private static final BlockingQueue<Message<HttpNotificationDispatcherMessage>>
      notificationMessageQueue = new LinkedBlockingQueue<>();

  private static final String WORKSPACES_URI = "http://localhost:8080/workspaces/";
  private static final String WORKSPACE_NAME = "test";

  /**
   * setup method.
   *
   * @param vertx vertx
   * @param ctx ctx
   */
  @BeforeEach
  public void setUp(final Vertx vertx, final VertxTestContext ctx) {
    notificationMessageQueue.clear();
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
        .<String, WebSubConfig>getLocalMap("notification-config")
        .put("default", notificationConfig);
    final var notificationMessagebox = new HttpNotificationDispatcherMessagebox(
        vertx.eventBus(),
        notificationConfig
    );
    notificationMessagebox.init();
    notificationMessagebox.receiveMessages(notificationMessageQueue::add);
    this.storeMessagebox = new RdfStoreMessagebox(vertx.eventBus());
    vertx.deployVerticle(new RdfStoreVerticle(), ctx.succeedingThenComplete());
  }

  @AfterEach
  public void tearDown(final Vertx vertx, final VertxTestContext ctx) {
    vertx.close(ctx.succeedingThenComplete());
  }

  @Test
  public void testGetWorkspacesNoWorkspaces(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var noContainedWorkspaces = Files.readString(
        Path.of(ClassLoader.getSystemResource("no_contained_workspaces.ttl").toURI()),
        StandardCharsets.UTF_8
    );
    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.GetWorkspaces("http://localhost:8080/"))
        .onSuccess(r -> Assertions.assertEquals(noContainedWorkspaces,r.body()))
        .onComplete(ctx.succeedingThenComplete());
  }


  @Test
  public void testGetWorkspacesTopLevelWorkspaces(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var twoContainedWorkspaces = Files.readString(
        Path.of(ClassLoader.getSystemResource("two_contained_workspaces.ttl").toURI()),
        StandardCharsets.UTF_8
    ).replaceAll(" ","");
    final var workspaceInput = Files.readString(
        Path.of(ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()),
        StandardCharsets.UTF_8
    );
    final var workspaceRepresentation = Files.readString(
        Path.of(ClassLoader.getSystemResource("test_getEntityIri_testWorkspace_td.ttl").toURI()),
        StandardCharsets.UTF_8
    );

    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.CreateWorkspace(
            WORKSPACES_URI,
            WORKSPACE_NAME,
            Optional.empty(),
            workspaceInput
        )).onSuccess(r -> Assertions.assertEquals(
            workspaceRepresentation,
            r.body(),
            "Representations should be equal"
        ));
    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.CreateWorkspace(
            WORKSPACES_URI,
            WORKSPACE_NAME + 2,
            Optional.empty(),
            workspaceInput
        ));

    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.GetWorkspaces("http://localhost:8080/"))
        .onSuccess(r -> Assertions.assertEquals(
            twoContainedWorkspaces,r.body().replaceAll(" ","")))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testGetSubWorkspacesNoSubWorkspaces(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var noContainedWorkspaces = Files.readString(
        Path.of(ClassLoader.getSystemResource("no_contained_subworkspaces.ttl").toURI()),
        StandardCharsets.UTF_8
    ).replaceAll(" ","");
    final var workspaceInput = Files.readString(
        Path.of(ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()),
        StandardCharsets.UTF_8
    );
    final var workspaceRepresentation = Files.readString(
        Path.of(ClassLoader.getSystemResource("test_getEntityIri_testWorkspace_td.ttl").toURI()),
        StandardCharsets.UTF_8
    );

    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.CreateWorkspace(
            WORKSPACES_URI,
            WORKSPACE_NAME,
            Optional.empty(),
            workspaceInput
        )).onSuccess(r -> Assertions.assertEquals(
            workspaceRepresentation,
            r.body(),
            "Representations should be equal"
        ));

    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.
            GetWorkspaces(WORKSPACES_URI + WORKSPACE_NAME))
        .onSuccess(r -> Assertions.assertEquals(
            noContainedWorkspaces,r.body().replaceAll(" ","")))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testGetSubWorkspacesTwoSubWorkspaces(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var twoContainedWorkspaces = Files.readString(
        Path.of(ClassLoader.getSystemResource("two_contained_subworkspaces.ttl").toURI()),
        StandardCharsets.UTF_8
    ).replaceAll(" ","");
    final var workspaceInput = Files.readString(
        Path.of(ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()),
        StandardCharsets.UTF_8
    );
    final var workspaceRepresentation = Files.readString(
        Path.of(ClassLoader.getSystemResource("test_getEntityIri_testWorkspace_td.ttl").toURI()),
        StandardCharsets.UTF_8
    );

    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.CreateWorkspace(
            WORKSPACES_URI,
            WORKSPACE_NAME,
            Optional.empty(),
            workspaceInput
        )).onSuccess(r -> Assertions.assertEquals(
            workspaceRepresentation,
            r.body(),
            "Representations should be equal"
        ));
    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.CreateWorkspace(
            WORKSPACES_URI,
            WORKSPACE_NAME + "2",
            Optional.of(WORKSPACES_URI + WORKSPACE_NAME),
            workspaceInput
        ));
    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.CreateWorkspace(
            WORKSPACES_URI,
            WORKSPACE_NAME + "3",
            Optional.of(WORKSPACES_URI + WORKSPACE_NAME),
            workspaceInput
        ));

    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.
            GetWorkspaces(WORKSPACES_URI + WORKSPACE_NAME))
        .onSuccess(r -> Assertions.assertEquals(
            twoContainedWorkspaces,r.body().replaceAll(" ","")))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void getArtifactsInWorkspaceNoArtifacts(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var noContainedWorkspaces = Files.readString(
        Path.of(ClassLoader.getSystemResource("no_contained_subworkspaces.ttl").toURI()),
        StandardCharsets.UTF_8
    ).replaceAll(" ","");
    final var workspaceInput = Files.readString(
        Path.of(ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()),
        StandardCharsets.UTF_8
    );
    final var workspaceRepresentation = Files.readString(
        Path.of(ClassLoader.getSystemResource("test_getEntityIri_testWorkspace_td.ttl").toURI()),
        StandardCharsets.UTF_8
    );

    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.CreateWorkspace(
            WORKSPACES_URI,
            WORKSPACE_NAME,
            Optional.empty(),
            workspaceInput
        )).onSuccess(r -> Assertions.assertEquals(
            workspaceRepresentation,
            r.body(),
            "Representations should be equal"
        ));

    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.
            GetArtifacts(WORKSPACE_NAME))
        .onSuccess(r -> Assertions.assertEquals(
            noContainedWorkspaces,r.body().replaceAll(" ","")))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void getArtifactsInWorkspaceTwoArtifacts(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var twoContainedArtifacts = Files.readString(
        Path.of(ClassLoader.getSystemResource("two_contained_artifacts.ttl").toURI()),
        StandardCharsets.UTF_8
    ).replaceAll(" ","");
    final var workspaceInput = Files.readString(
        Path.of(ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()),
        StandardCharsets.UTF_8
    );
    final var workspaceRepresentation = Files.readString(
        Path.of(ClassLoader.getSystemResource("test_getEntityIri_testWorkspace_td.ttl").toURI()),
        StandardCharsets.UTF_8
    );
    final var artifactRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("c0_counter_artifact_td.ttl").toURI()),
            StandardCharsets.UTF_8
        );

    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.CreateWorkspace(
            WORKSPACES_URI,
            WORKSPACE_NAME,
            Optional.empty(),
            workspaceInput
        )).onSuccess(r -> Assertions.assertEquals(
            workspaceRepresentation,
            r.body(),
            "Representations should be equal"
        ));
    this.storeMessagebox.sendMessage(
        new RdfStoreMessage.CreateArtifact(
            WORKSPACES_URI + WORKSPACE_NAME + "/artifacts/",
            "c1",
            artifactRepresentation
        )
    );
    this.storeMessagebox.sendMessage(
        new RdfStoreMessage.CreateArtifact(
            WORKSPACES_URI + WORKSPACE_NAME + "/artifacts/",
            "c2",
            artifactRepresentation
        )
    );

    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.
            GetArtifacts(WORKSPACE_NAME))
        .onSuccess(r -> Assertions.assertEquals(
            twoContainedArtifacts,r.body().replaceAll(" ","")))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testGetEmptyPlatformResource(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var expectedPlatformRepresentation = Files.readString(
        Path.of(ClassLoader.getSystemResource("platform_td.ttl").toURI()),
        StandardCharsets.UTF_8
    );

    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.GetEntity("http://localhost:8080/"))
        .onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
            expectedPlatformRepresentation,
            r.body()
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testGetMissingEntity(final VertxTestContext ctx) {
    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.GetEntity("http://yggdrasil:8080/"))
        .onFailure(RdfStoreVerticleTestHelpers::assertNotFound)
        .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testMalformedUri(final VertxTestContext ctx) {
    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.GetEntity("nonexistent"))
        .onFailure(RdfStoreVerticleTestHelpers::assertBadRequest)
        .onComplete(ctx.failingThenComplete());
  }

  @Test
  public void testGetEntityIriNonExistent(final VertxTestContext ctx) {
    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.GetEntityIri(WORKSPACES_URI, "nonexistent"))
        .onSuccess(r -> Assertions.assertEquals(
            "nonexistent",
            r.body(),
            "URIs should be the same"
        )).onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testGetEntityIriExistsAlredy(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var workspaceInput = Files.readString(
        Path.of(ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()),
        StandardCharsets.UTF_8
    );
    final var workspaceRepresentation = Files.readString(
        Path.of(ClassLoader.getSystemResource("test_getEntityIri_testWorkspace_td.ttl").toURI()),
        StandardCharsets.UTF_8
    );

    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.CreateWorkspace(
            WORKSPACES_URI,
            WORKSPACE_NAME,
            Optional.empty(),
            workspaceInput
        )).onSuccess(r -> Assertions.assertEquals(
            workspaceRepresentation,
            r.body(),
            "Representations should be equal"
        ));

    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.GetEntity(
            "http://yggdrasil:8080/workspaces/test"
        )).onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
            workspaceRepresentation,
            r.body()
        ));

    // trying to get another thing with the same slug
    // should return a UUID that is not the slug
    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.GetEntityIri(
            WORKSPACES_URI,
            WORKSPACE_NAME
        )).onSuccess(r -> Assertions.assertNotEquals(
            WORKSPACE_NAME,
            r.body()
        ));
    ctx.completeNow();
  }

  /**
   * Both URIs /test and /test/ should return the representation of the same resource.
   *
   * @param ctx Test context
   * @throws URISyntaxException due to URI conversion
   * @throws IOException        due to reading in a file
   */
  @Test
  public void testDifferentUriEndingsForSameResult(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var workspaceInput = Files.readString(
        Path.of(ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()),
        StandardCharsets.UTF_8
    );
    final var workspaceRepresentation = Files.readString(
        Path.of(ClassLoader.getSystemResource("test_getEntityIri_testWorkspace_td.ttl").toURI()),
        StandardCharsets.UTF_8
    );

    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.CreateWorkspace(
            WORKSPACES_URI,
            WORKSPACE_NAME,
            Optional.empty(),
            workspaceInput
        )).onSuccess(r -> Assertions.assertEquals(
            workspaceRepresentation,
            r.body(),
            "Representations should be equal"
        ));

    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.GetEntity(
            "http://yggdrasil:8080/workspaces/test"
        )).onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
            workspaceRepresentation,
            r.body()
        ));

    this.storeMessagebox
        .sendMessage(new RdfStoreMessage.GetEntity(
            "http://yggdrasil:8080/workspaces/test/"
        )).onSuccess(r -> RdfStoreVerticleTestHelpers.assertEqualsThingDescriptions(
            workspaceRepresentation,
            r.body()
        ));

    ctx.completeNow();
  }
}
