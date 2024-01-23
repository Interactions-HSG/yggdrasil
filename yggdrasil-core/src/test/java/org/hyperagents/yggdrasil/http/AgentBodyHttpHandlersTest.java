package org.hyperagents.yggdrasil.http;

import com.google.common.net.HttpHeaders;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.WebSubConfig;
import org.hyperagents.yggdrasil.utils.impl.EnvironmentConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.WebSubConfigImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@ExtendWith(VertxExtension.class)
public class AgentBodyHttpHandlersTest {
  private static final int TEST_PORT = 8080;
  private static final String TEST_HOST = "localhost";
  private static final String TEST_AGENT_ID = "test_agent";
  private static final String TURTLE_CONTENT_TYPE = "text/turtle";
  private static final String BODIES_PATH = "/workspaces/test/agents/";
  private static final String BODY_PATH = BODIES_PATH + TEST_AGENT_ID;
  private static final String NONEXISTENT_NAME = "nonexistent";
  private static final String BODY_FILE = "test_agent_body.ttl";

  private final BlockingQueue<Message<RdfStoreMessage>> storeMessageQueue;
  private WebClient client;
  private HttpServerVerticleTestHelper helper;

  public AgentBodyHttpHandlersTest() {
    this.storeMessageQueue = new LinkedBlockingQueue<>();
  }

  @BeforeEach
  public void setUp(final Vertx vertx, final VertxTestContext ctx) {
    this.client = WebClient.create(vertx);
    this.helper = new HttpServerVerticleTestHelper(this.client, this.storeMessageQueue);
    final var httpConfig = new HttpInterfaceConfigImpl(JsonObject.of());
    vertx.sharedData()
         .<String, HttpInterfaceConfig>getLocalMap("http-config")
         .put("default", httpConfig);
    vertx.sharedData()
         .<String, EnvironmentConfig>getLocalMap("environment-config")
         .put("default",
              new EnvironmentConfigImpl(JsonObject.of(
                "environment-config",
                JsonObject.of("enabled", true)
              )));
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
    final var storeMessagebox = new RdfStoreMessagebox(vertx.eventBus());
    storeMessagebox.init();
    storeMessagebox.receiveMessages(this.storeMessageQueue::add);
    new HttpNotificationDispatcherMessagebox(vertx.eventBus(), notificationConfig).init();
    vertx.deployVerticle(new HttpServerVerticle(), ctx.succeedingThenComplete());
  }

  @AfterEach
  public void tearDown(final Vertx vertx, final VertxTestContext ctx) {
    vertx.close(ctx.succeedingThenComplete());
  }

  @Test
  public void testGetBodySucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException, InterruptedException {
    this.helper.testGetResourceSucceeds(ctx, BODY_FILE, BODY_PATH);
  }

  @Test
  public void testGetBodyRedirectsWithSlash(final VertxTestContext ctx) {
    this.helper.testResourceRequestRedirectsWithAddedSlash(
        ctx,
        HttpMethod.GET,
        BODY_PATH + "/"
    );
  }

  @Test
  public void testGetBodyFailsWithNotFound(final VertxTestContext ctx)
      throws InterruptedException {
    this.helper.testResourceRequestFailsWithNotFound(
        ctx,
        BODIES_PATH + NONEXISTENT_NAME,
        this.client.get(TEST_PORT, TEST_HOST, BODIES_PATH + NONEXISTENT_NAME).send()
    );
  }

  @Test
  public void testPutTurtleArtifactSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException, InterruptedException {
    this.helper.testPutTurtleResourceSucceeds(
        ctx,
        BODY_PATH,
        BODY_FILE
    );
  }

  @Test
  public void testPutTurtleArtifactFailsWithNotFound(final VertxTestContext ctx)
      throws URISyntaxException, IOException, InterruptedException {
    this.helper.testResourceRequestFailsWithNotFound(
        ctx,
        BODIES_PATH + NONEXISTENT_NAME,
        this.client.put(TEST_PORT, TEST_HOST, BODIES_PATH + NONEXISTENT_NAME)
                   .putHeader("X-Agent-WebID", TEST_AGENT_ID)
                   .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
                   .sendBuffer(Buffer.buffer(Files.readString(
                     Path.of(ClassLoader.getSystemResource(BODY_FILE).toURI()),
                     StandardCharsets.UTF_8
                   )))
    );
  }

  @Test
  public void testPutTurtleArtifactFailsWithoutWebId(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    this.helper.testResourceRequestFailsWithoutWebId(
        ctx,
        this.client.put(TEST_PORT, TEST_HOST, BODY_PATH)
                   .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
                   .sendBuffer(Buffer.buffer(Files.readString(
                     Path.of(ClassLoader.getSystemResource(BODY_FILE).toURI()),
                     StandardCharsets.UTF_8
                   )))
    );
  }

  @Test
  public void testPutTurtleArtifactFailsWithoutContentType(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    this.helper.testResourceRequestFailsWithoutContentType(
        ctx,
        HttpMethod.PUT,
        BODY_PATH,
        Buffer.buffer(Files.readString(
          Path.of(ClassLoader.getSystemResource(BODY_FILE).toURI()),
          StandardCharsets.UTF_8
        ))
    );
  }

  @Test
  public void testPutTurtleArtifactRedirectsWithSlash(final VertxTestContext ctx) {
    this.helper.testResourceRequestRedirectsWithAddedSlash(
        ctx,
        HttpMethod.PUT,
        BODY_PATH
    );
  }
}
