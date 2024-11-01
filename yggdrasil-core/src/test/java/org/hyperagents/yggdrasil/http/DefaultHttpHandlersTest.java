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
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.http.HttpStatus;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * testclass.
 */
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@ExtendWith(VertxExtension.class)
public class DefaultHttpHandlersTest {
  private static final int TEST_PORT = 8080;
  private static final String TEST_HOST = "localhost";
  private static final String AGENT_WEBID = "X-Agent-WebID";
  private static final String TEST_AGENT_ID = "test_agent";
  private static final String SLUG_HEADER = "Slug";
  private static final String MAIN_WORKSPACE_NAME = "test";
  private static final String SUB_WORKSPACE_NAME = "sub";
  private static final String TURTLE_CONTENT_TYPE = "text/turtle";
  private static final String WORKSPACES_PATH = "/workspaces/";
  private static final String MAIN_WORKSPACE_PATH = WORKSPACES_PATH + MAIN_WORKSPACE_NAME;
  private static final String ARTIFACTS_PATH = MAIN_WORKSPACE_PATH + "/artifacts/";
  private static final String COUNTER_ARTIFACT_NAME = "c0";
  private static final String COUNTER_ARTIFACT_PATH = ARTIFACTS_PATH + COUNTER_ARTIFACT_NAME;
  private static final String CREATED_STATUS_MESSAGE = "Status code should be CREATED";
  private static final String NAMES_EQUAL_MESSAGE = "The names should be equal";
  private static final String URIS_EQUAL_MESSAGE = "The URIs should be equal";
  private static final String TDS_EQUAL_MESSAGE = "The thing descriptions should be equal";
  private static final String TEST_WORKSPACE_FILE = "test_workspace_td.ttl";
  private static final String NONEXISTENT_NAME = "nonexistent";
  private static final String COUNTER_ARTIFACT_FILE = "c0_counter_artifact_td.ttl";

  private final BlockingQueue<Message<RdfStoreMessage>> storeMessageQueue;
  private WebClient client;
  private HttpServerVerticleTestHelper helper;

  public DefaultHttpHandlersTest() {
    this.storeMessageQueue = new LinkedBlockingQueue<>();

  }

  /**
   * setup method.
   *
   * @param vertx vertx
   * @param ctx ctx
   */
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
                JsonObject.of("enabled", false, "ontology", "td")
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
  public void testGetRootSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException, InterruptedException {
    this.helper.testGetResourceSucceeds(ctx, "platform_td.ttl", "/");
  }

  @Test
  public void testGetWorkspaceSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException, InterruptedException {
    this.helper.testGetResourceSucceeds(ctx, TEST_WORKSPACE_FILE, MAIN_WORKSPACE_PATH);
  }

  @Test
  public void testGetWorkspaceRedirectsWithSlash(final VertxTestContext ctx) {
    this.helper.testResourceRequestRedirectsWithAddedSlash(
        ctx,
        HttpMethod.GET,
        MAIN_WORKSPACE_PATH + "/"
    );
  }

  @Test
  public void testGetWorkspaceFailsWithNotFound(final VertxTestContext ctx)
      throws InterruptedException {
    this.helper.testResourceRequestFailsWithNotFound(
        ctx,
        WORKSPACES_PATH + NONEXISTENT_NAME,
        this.client.get(TEST_PORT, TEST_HOST, WORKSPACES_PATH + NONEXISTENT_NAME).send()
    );
  }

  @Test
  public void testGetArtifactSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException, InterruptedException {
    this.helper.testGetResourceSucceeds(ctx, COUNTER_ARTIFACT_FILE, COUNTER_ARTIFACT_PATH);
  }

  @Test
  public void testGetArtifactRedirectsWithSlash(final VertxTestContext ctx) {
    this.helper.testResourceRequestRedirectsWithAddedSlash(
        ctx,
        HttpMethod.GET,
        COUNTER_ARTIFACT_PATH + "/"
    );
  }

  @Test
  public void testGetArtifactFailsWithNotFound(final VertxTestContext ctx)
      throws InterruptedException {
    this.helper.testResourceRequestFailsWithNotFound(
        ctx,
        ARTIFACTS_PATH + NONEXISTENT_NAME,
        this.client.get(TEST_PORT, TEST_HOST, ARTIFACTS_PATH + NONEXISTENT_NAME).send()
    );
  }

  @Test
  public void testPostTurtleWorkspacesSucceeds(final VertxTestContext ctx)
      throws InterruptedException, URISyntaxException, IOException {
    final var input =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("test_workspace_turtle_input.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var expectedRepresentation =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("test_workspace_turtle_output.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var request = this.client.post(TEST_PORT, TEST_HOST, WORKSPACES_PATH)
        .putHeader(AGENT_WEBID, TEST_AGENT_ID)
        .putHeader(SLUG_HEADER, MAIN_WORKSPACE_NAME)
        .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
        .sendBuffer(Buffer.buffer(input));
    final var firstMessage = this.storeMessageQueue.take();
    firstMessage.reply(MAIN_WORKSPACE_NAME);
    final var message = this.storeMessageQueue.take();
    final var createResourceMessage = (RdfStoreMessage.CreateWorkspace) message.body();
    Assertions.assertEquals(
        this.helper.getUri(WORKSPACES_PATH),
        createResourceMessage.requestUri(),
        URIS_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        MAIN_WORKSPACE_NAME,
        createResourceMessage.workspaceName(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        Optional.empty(),
        createResourceMessage.parentWorkspaceUri(),
        "There should not be any URI for the parent workspace"
    );
    Assertions.assertEquals(
        expectedRepresentation,
        createResourceMessage.workspaceRepresentation(),
        TDS_EQUAL_MESSAGE
    );
    message.reply(input);
    request
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_CREATED,
              r.statusCode(),
              CREATED_STATUS_MESSAGE
          );
          Assertions.assertEquals(
              TURTLE_CONTENT_TYPE,
              r.getHeader(HttpHeaders.CONTENT_TYPE),
              "The content type should be text/turtle"
          );
          Assertions.assertEquals(
              input,
              r.bodyAsString(),
              TDS_EQUAL_MESSAGE
          );
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testPostTurtleWorkspacesFailsWithoutWebId(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    this.helper.testResourceRequestFailsWithoutWebId(
        ctx,
        this.client.post(TEST_PORT, TEST_HOST, WORKSPACES_PATH)
            .putHeader(SLUG_HEADER, MAIN_WORKSPACE_NAME)
            .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
            .sendBuffer(Buffer.buffer(Files.readString(Path.of(
                    ClassLoader.getSystemResource(TEST_WORKSPACE_FILE).toURI()),
                StandardCharsets.UTF_8
            )))
    );
  }

  @Test
  public void testPostTurtleSubWorkspaceSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException, InterruptedException {

    final var input =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("sub_workspace_turtle_input.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var output =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("sub_workspace_turtle_output.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var request = this.client.post(TEST_PORT, TEST_HOST, MAIN_WORKSPACE_PATH)
        .putHeader(AGENT_WEBID, TEST_AGENT_ID)
        .putHeader(SLUG_HEADER, SUB_WORKSPACE_NAME)
        .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
        .sendBuffer(Buffer.buffer(input));
    final var firstMessage = this.storeMessageQueue.take();
    firstMessage.reply(SUB_WORKSPACE_NAME);
    final var message = this.storeMessageQueue.take();
    final var createResourceMessage = (RdfStoreMessage.CreateWorkspace) message.body();

    Assertions.assertEquals(
        this.helper.getUri(WORKSPACES_PATH),
        createResourceMessage.requestUri(),
        URIS_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        SUB_WORKSPACE_NAME,
        createResourceMessage.workspaceName(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        Optional.of(this.helper.getUri(MAIN_WORKSPACE_PATH)),
        createResourceMessage.parentWorkspaceUri(),
        URIS_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        output,
        createResourceMessage.workspaceRepresentation(),
        TDS_EQUAL_MESSAGE
    );
    message.reply(output);
    request
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_CREATED,
              r.statusCode(),
              CREATED_STATUS_MESSAGE
          );
          Assertions.assertEquals(
              TURTLE_CONTENT_TYPE,
              r.getHeader(HttpHeaders.CONTENT_TYPE),
              "The content type should be text/turtle"
          );
          Assertions.assertEquals(
              output,
              r.bodyAsString(),
              TDS_EQUAL_MESSAGE
          );
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testPostTurtleSubWorkspaceFailsWithoutWebId(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    this.helper.testResourceRequestFailsWithoutWebId(
        ctx,
        this.client.post(TEST_PORT, TEST_HOST, MAIN_WORKSPACE_PATH)
            .putHeader(SLUG_HEADER, SUB_WORKSPACE_NAME)
            .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
            .sendBuffer(Buffer.buffer(Files.readString(Path.of(
                    ClassLoader.getSystemResource("sub_workspace_td.ttl").toURI()),
                StandardCharsets.UTF_8
            )))
    );
  }

  @Test
  public void testPostTurtleArtifactSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException, InterruptedException {
    final var input =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("c0_artifact_turtle_input.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var intermediateOutput =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("c0_artifact_turtle_intermediate_output.ttl")
                .toURI()),
            StandardCharsets.UTF_8
        );
    final var output =
        Files.readString(
            Path.of(ClassLoader.getSystemResource("c0_artifact_turtle_output.ttl").toURI()),
            StandardCharsets.UTF_8
        );
    final var request = this.client.post(TEST_PORT, TEST_HOST, ARTIFACTS_PATH)
        .putHeader(AGENT_WEBID, TEST_AGENT_ID)
        .putHeader(SLUG_HEADER, COUNTER_ARTIFACT_NAME)
        .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
        .sendBuffer(Buffer.buffer(input));
    final var firstMessage = this.storeMessageQueue.take();
    firstMessage.reply(COUNTER_ARTIFACT_NAME);
    final var message = this.storeMessageQueue.take();
    final var createResourceMessage = (RdfStoreMessage.CreateArtifact) message.body();
    Assertions.assertEquals(
        this.helper.getUri(ARTIFACTS_PATH),
        createResourceMessage.requestUri(),
        URIS_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        COUNTER_ARTIFACT_NAME,
        createResourceMessage.artifactName(),
        NAMES_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        intermediateOutput,
        createResourceMessage.artifactRepresentation(),
        TDS_EQUAL_MESSAGE
    );
    message.reply(intermediateOutput);
    final var secondMessage = this.storeMessageQueue.take();
    final var updateResourceMessage = (RdfStoreMessage.UpdateEntity) secondMessage.body();
    Assertions.assertEquals(
        input,
        updateResourceMessage.entityRepresentation(),
        TDS_EQUAL_MESSAGE
    );
    Assertions.assertEquals(
        this.helper.getUri(ARTIFACTS_PATH + COUNTER_ARTIFACT_NAME),
        updateResourceMessage.requestUri(),
        URIS_EQUAL_MESSAGE
    );
    secondMessage.reply(output);
    request
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_CREATED,
              r.statusCode(),
              CREATED_STATUS_MESSAGE
          );
          Assertions.assertEquals(
              TURTLE_CONTENT_TYPE,
              r.getHeader(HttpHeaders.CONTENT_TYPE),
              "The content type should be text/turtle"
          );
          Assertions.assertEquals(
              output,
              r.bodyAsString(),
              TDS_EQUAL_MESSAGE
          );
        })
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testPostTurtleArtifactFailsWithoutWebId(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    this.helper.testResourceRequestFailsWithoutWebId(
        ctx,
        this.client.post(TEST_PORT, TEST_HOST, ARTIFACTS_PATH)
            .putHeader(SLUG_HEADER, SUB_WORKSPACE_NAME)
            .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
            .sendBuffer(Buffer.buffer(Files.readString(Path.of(
                    ClassLoader.getSystemResource("sub_workspace_td.ttl").toURI()),
                StandardCharsets.UTF_8
            )))
    );
  }

  @Test
  public void testPutTurtleWorkspaceSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException, InterruptedException {
    this.helper.testPutTurtleResourceSucceeds(
        ctx,
        MAIN_WORKSPACE_PATH,
        TEST_WORKSPACE_FILE
    );
  }

  @Test
  public void testPutTurtleWorkspaceFailsWithNotFound(final VertxTestContext ctx)
      throws URISyntaxException, IOException, InterruptedException {
    this.helper.testResourceRequestFailsWithNotFound(
        ctx,
        WORKSPACES_PATH + NONEXISTENT_NAME,
        this.client.put(TEST_PORT, TEST_HOST, WORKSPACES_PATH + NONEXISTENT_NAME)
            .putHeader(AGENT_WEBID, TEST_AGENT_ID)
            .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
            .sendBuffer(Buffer.buffer(Files.readString(
                Path.of(ClassLoader.getSystemResource(TEST_WORKSPACE_FILE).toURI()),
                StandardCharsets.UTF_8
            )))
    );
  }

  @Test
  public void testPutTurtleWorkspaceFailsWithoutWebId(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    this.helper.testResourceRequestFailsWithoutWebId(
        ctx,
        this.client.put(TEST_PORT, TEST_HOST, MAIN_WORKSPACE_PATH)
            .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
            .sendBuffer(Buffer.buffer(Files.readString(
                Path.of(ClassLoader.getSystemResource(TEST_WORKSPACE_FILE).toURI()),
                StandardCharsets.UTF_8
            )))
    );
  }

  @Test
  public void testPutTurtleWorkspaceFailsWithoutContentType(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    this.helper.testResourceRequestFailsWithoutContentType(
        ctx,
        HttpMethod.PUT,
        MAIN_WORKSPACE_PATH,
        Buffer.buffer(Files.readString(
            Path.of(ClassLoader.getSystemResource(TEST_WORKSPACE_FILE).toURI()),
            StandardCharsets.UTF_8
        ))
    );
  }

  @Test
  public void testPutTurtleWorkspaceRedirectsWithSlash(final VertxTestContext ctx) {
    this.helper.testResourceRequestRedirectsWithAddedSlash(
        ctx,
        HttpMethod.PUT,
        MAIN_WORKSPACE_PATH
    );
  }

  @Test
  public void testPutTurtleArtifactSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException, InterruptedException {
    this.helper.testPutTurtleResourceSucceeds(
        ctx,
        COUNTER_ARTIFACT_PATH,
        COUNTER_ARTIFACT_FILE
    );
  }

  @Test
  public void testPutTurtleArtifactFailsWithNotFound(final VertxTestContext ctx)
      throws URISyntaxException, IOException, InterruptedException {
    this.helper.testResourceRequestFailsWithNotFound(
        ctx,
        ARTIFACTS_PATH + NONEXISTENT_NAME,
        this.client.put(TEST_PORT, TEST_HOST, ARTIFACTS_PATH + NONEXISTENT_NAME)
            .putHeader(AGENT_WEBID, TEST_AGENT_ID)
            .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
            .sendBuffer(Buffer.buffer(Files.readString(
                Path.of(ClassLoader.getSystemResource(COUNTER_ARTIFACT_FILE).toURI()),
                StandardCharsets.UTF_8
            )))
    );
  }

  @Test
  public void testPutTurtleArtifactFailsWithoutWebId(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    this.helper.testResourceRequestFailsWithoutWebId(
        ctx,
        this.client.put(TEST_PORT, TEST_HOST, COUNTER_ARTIFACT_PATH)
            .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
            .sendBuffer(Buffer.buffer(Files.readString(
                Path.of(ClassLoader.getSystemResource(COUNTER_ARTIFACT_FILE).toURI()),
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
        COUNTER_ARTIFACT_PATH,
        Buffer.buffer(Files.readString(
            Path.of(ClassLoader.getSystemResource(COUNTER_ARTIFACT_FILE).toURI()),
            StandardCharsets.UTF_8
        ))
    );
  }

  @Test
  public void testPutTurtleArtifactRedirectsWithSlash(final VertxTestContext ctx) {
    this.helper.testResourceRequestRedirectsWithAddedSlash(
        ctx,
        HttpMethod.PUT,
        COUNTER_ARTIFACT_PATH
    );
  }

  @Test
  public void testDeleteTurtleWorkspaceSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException, InterruptedException {
    this.helper.testDeleteTurtleResourceSucceeds(
        ctx,
        MAIN_WORKSPACE_PATH,
        TEST_WORKSPACE_FILE
    );
  }

  @Test
  public void testDeleteTurtleWorkspaceFailsWithNotFound(final VertxTestContext ctx)
      throws InterruptedException {
    this.helper.testResourceRequestFailsWithNotFound(
        ctx,
        WORKSPACES_PATH + NONEXISTENT_NAME,
        this.client.delete(TEST_PORT, TEST_HOST, WORKSPACES_PATH + NONEXISTENT_NAME)
            .putHeader(AGENT_WEBID, TEST_AGENT_ID)
            .send()
    );
  }

  @Test
  public void testDeleteTurtleWorkspaceFailsWithoutWebId(final VertxTestContext ctx) {
    this.helper.testResourceRequestFailsWithoutWebId(
        ctx,
        this.client.delete(TEST_PORT, TEST_HOST, MAIN_WORKSPACE_PATH).send()
    );
  }

  @Test
  public void testDeleteTurtleWorkspaceRedirectsWithSlash(final VertxTestContext ctx) {
    this.helper.testResourceRequestRedirectsWithAddedSlash(
        ctx,
        HttpMethod.DELETE,
        MAIN_WORKSPACE_PATH
    );
  }

  @Test
  public void testDeleteTurtleArtifactSucceeds(final VertxTestContext ctx)
      throws URISyntaxException, IOException, InterruptedException {
    this.helper.testDeleteTurtleResourceSucceeds(
        ctx,
        COUNTER_ARTIFACT_PATH,
        COUNTER_ARTIFACT_FILE
    );
  }

  @Test
  public void testDeleteTurtleArtifactFailsWithNotFound(final VertxTestContext ctx)
      throws InterruptedException {
    this.helper.testResourceRequestFailsWithNotFound(
        ctx,
        ARTIFACTS_PATH + NONEXISTENT_NAME,
        this.client.delete(TEST_PORT, TEST_HOST, ARTIFACTS_PATH + NONEXISTENT_NAME)
            .putHeader(AGENT_WEBID, TEST_AGENT_ID)
            .send()
    );
  }

  @Test
  public void testDeleteTurtleArtifactFailsWithoutWebId(final VertxTestContext ctx) {
    this.helper.testResourceRequestFailsWithoutWebId(
        ctx,
        this.client.delete(TEST_PORT, TEST_HOST, COUNTER_ARTIFACT_PATH).send()
    );
  }

  @Test
  public void testDeleteTurtleArtifactRedirectsWithSlash(final VertxTestContext ctx) {
    this.helper.testResourceRequestRedirectsWithAddedSlash(
        ctx,
        HttpMethod.DELETE,
        COUNTER_ARTIFACT_PATH
    );
  }

  @Test
  public void testEntityCorsHeaders(final VertxTestContext ctx) throws InterruptedException {
    final var request = this.client.get(TEST_PORT, TEST_HOST, "/").send();
    this.storeMessageQueue.take().reply(null);
    request
        .onSuccess(r -> {
          Assertions.assertEquals(
              HttpStatus.SC_OK,
              r.statusCode(),
              "Status code should be OK"
          );
          Assertions.assertEquals(
              "*",
              r.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN),
              "CORS origin should be open"
          );
          Assertions.assertEquals(
              "true",
              r.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS),
              "CORS credentials should be allowed"
          );
          Assertions.assertTrue(
              r.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)
                  .contains(HttpMethod.GET.name()),
              "CORS should permit GET on entities"
          );
          Assertions.assertTrue(
              r.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)
                  .contains(HttpMethod.POST.name()),
              "CORS should permit POST on entities"
          );
          Assertions.assertTrue(
              r.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)
                  .contains(HttpMethod.PUT.name()),
              "CORS should permit PUT on entities"
          );
          Assertions.assertTrue(
              r.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)
                  .contains(HttpMethod.DELETE.name()),
              "CORS should permit DELETE on entities"
          );
          Assertions.assertTrue(
              r.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)
                  .contains(HttpMethod.HEAD.name()),
              "CORS should permit HEAD on entities"
          );
          Assertions.assertTrue(
              r.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)
                  .contains(HttpMethod.OPTIONS.name()),
              "CORS should permit OPTIONS on entities"
          );
        })
        .onComplete(ctx.succeedingThenComplete());
  }
}
