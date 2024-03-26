package org.hyperagents.yggdrasil.store;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.impl.EnvironmentConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@ExtendWith(VertxExtension.class)
public class RdfStoreVerticleHMASGetTest {
  private RdfStoreMessagebox storeMessagebox;

  @BeforeEach
  public void setUp(final Vertx vertx, final VertxTestContext ctx) {
    vertx.sharedData()
         .<String, HttpInterfaceConfig>getLocalMap("http-config")
         .put("default", new HttpInterfaceConfigImpl(JsonObject.of()));
    vertx.sharedData()
         .getLocalMap("environment-config")
         .put("default",
              new EnvironmentConfigImpl(JsonObject.of(
                "environment-config",
                JsonObject.of(
                  "enabled",
                  true
                )
              )));
    this.storeMessagebox = new RdfStoreMessagebox(vertx.eventBus());
    vertx.deployVerticle(new RdfStoreVerticleHMAS(), ctx.succeedingThenComplete());
  }

  @AfterEach
  public void tearDown(final Vertx vertx, final VertxTestContext ctx) {
    vertx.close(ctx.succeedingThenComplete());
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
}
