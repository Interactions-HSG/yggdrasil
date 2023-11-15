package org.hyperagents.yggdrasil.cartago;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.hyperagents.yggdrasil.cartago.artifacts.Matcher;
import org.hyperagents.yggdrasil.eventbus.messageboxes.CartagoMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class CartagoProjectInitTest {
  private static final String AGENT_IRI = "http://localhost:8080/agents/test";
  private static final String MATCHER_SEMANTIC_TYPE = "http://example.org/Matcher";
  private static final String TDS_EQUAL_MESSAGE = "The Thing Descriptions should be equal";
  private static final String NAMES_EQUAL_MESSAGE = "The names should be equal";
  public static final String IRIS_EQUAL_MESSAGE = "The IRIs should be equal";
  private static final String OPERATION_SUCCESS_MESSAGE =
      "The operation should have succeeded with an Ok status code";

  private final BlockingQueue<RdfStoreMessage> storeMessageQueue;
  private CartagoMessagebox cartagoMessagebox;

  public CartagoProjectInitTest() {
    this.storeMessageQueue = new LinkedBlockingQueue<>();
  }

  @BeforeEach
  public void setUp(final Vertx vertx) {
    this.cartagoMessagebox = new CartagoMessagebox(vertx.eventBus());
    final var storeMessagebox = new RdfStoreMessagebox(vertx.eventBus());
    storeMessagebox.init();
    storeMessagebox.receiveMessages(m -> this.storeMessageQueue.add(m.body()));
  }

  @Test
  public void testConfigurationIsCorrectlyLoaded(final Vertx vertx, final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var expectedThingDescription =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("default_agent_body.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    vertx
        .deployVerticle(
          new CartagoVerticle(),
          new DeploymentOptions()
            .setConfig(new JsonObject(Map.of(
              "known-artifacts",
              Map.of(MATCHER_SEMANTIC_TYPE, Matcher.class.getCanonicalName()),
              "app-conf-file",
              "src/test/resources/default_conf.jcm"
            )))
        )
        .onSuccess(i -> {
          try {
            final var defaultAgentBodyCreationMessage =
                (RdfStoreMessage.CreateEntity) this.storeMessageQueue.take();
            Assertions.assertEquals(
                "http://localhost:8080/workspaces/test/artifacts",
                defaultAgentBodyCreationMessage.requestUri(),
                IRIS_EQUAL_MESSAGE
            );
            Assertions.assertEquals(
                "body_http://www.example.org/agents/default",
                defaultAgentBodyCreationMessage.entityName(),
                NAMES_EQUAL_MESSAGE
            );
            Assertions.assertEquals(
                expectedThingDescription,
                defaultAgentBodyCreationMessage.entityRepresentation(),
                TDS_EQUAL_MESSAGE
            );
            final var defaultAgentHypermediaBodyCreationMessage =
                (RdfStoreMessage.CreateEntity) this.storeMessageQueue.take();
            Assertions.assertEquals(
                "http://localhost:8080/workspaces/test/artifacts/",
                defaultAgentHypermediaBodyCreationMessage.requestUri(),
                IRIS_EQUAL_MESSAGE
            );
            Assertions.assertEquals(
                "hypermedia_body_1",
                defaultAgentHypermediaBodyCreationMessage.entityName(),
                NAMES_EQUAL_MESSAGE
            );
            Assertions.assertEquals(
                expectedThingDescription,
                defaultAgentHypermediaBodyCreationMessage.entityRepresentation(),
                TDS_EQUAL_MESSAGE
            );
          } catch (final InterruptedException e) {
            ctx.failNow(e);
          }
          this.cartagoMessagebox
              .sendMessage(new CartagoMessage.DoAction(
                AGENT_IRI,
                "test",
                "m0",
                "matches",
                Optional.of(CartagoDataBundle.toJson(List.of("42")))
              ))
              .compose(r -> {
                Assertions.assertEquals(
                    "true",
                    r.body(),
                    OPERATION_SUCCESS_MESSAGE
                );
                return this.cartagoMessagebox.sendMessage(new CartagoMessage.DoAction(
                  AGENT_IRI,
                  "test",
                  "m1",
                  "matches",
                  Optional.of(CartagoDataBundle.toJson(List.of("42")))
                ));
              })
              .onSuccess(r -> {
                Assertions.assertEquals(
                    "false",
                    r.body(),
                    OPERATION_SUCCESS_MESSAGE
                );
                vertx.undeploy(i, ctx.succeedingThenComplete());
              });
        });
  }

  @Test
  public void testBadlyFormattedConfigurationDoesNotBreakInitialization(
      final Vertx vertx,
      final VertxTestContext ctx
  ) {
    vertx
        .deployVerticle(
            new CartagoVerticle(),
            new DeploymentOptions()
              .setConfig(new JsonObject(Map.of(
                "known-artifacts",
                Map.of(MATCHER_SEMANTIC_TYPE, Matcher.class.getCanonicalName()),
                "app-conf-file",
                "src/test/resources/bad_conf.jcm"
              )))
        )
        .onSuccess(i -> vertx.undeploy(i, ctx.succeedingThenComplete()));
  }
}
