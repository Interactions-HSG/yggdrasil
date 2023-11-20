package org.hyperagents.yggdrasil.http;

import com.google.common.net.HttpHeaders;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
  private static final String OK_STATUS_MESSAGE = "Status code should be OK";

  private final BlockingQueue<Message<RdfStoreMessage>> storeMessageQueue;
  private WebClient client;
  private HttpServerVerticleTestHelper helper;

  public DefaultHttpHandlersTest() {
    this.storeMessageQueue = new LinkedBlockingQueue<>();
  }

  @BeforeEach
  public void setUp(final Vertx vertx, final VertxTestContext ctx) {
    this.client = WebClient.create(vertx);
    this.helper = new HttpServerVerticleTestHelper(this.client, this.storeMessageQueue);
    final var storeMessagebox = new RdfStoreMessagebox(vertx.eventBus());
    storeMessagebox.init();
    storeMessagebox.receiveMessages(this.storeMessageQueue::add);
    vertx.deployVerticle(new HttpServerVerticle(), ctx.succeedingThenComplete());
  }

  @AfterEach
  public void tearDown(final Vertx vertx, final VertxTestContext ctx) {
    vertx.close(ctx.succeedingThenComplete());
  }

  @Test
  public void testGetRootSucceeds(final VertxTestContext ctx) {
    this.helper.testGetResourceSucceeds(ctx, "platform_td.ttl", "/");
  }

  @Test
  public void testGetWorkspaceSucceeds(final VertxTestContext ctx) {
    this.helper.testGetResourceSucceeds(ctx, "test_workspace_td.ttl", MAIN_WORKSPACE_PATH);
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
  public void testGetWorkspaceFailsWithNotFound(final VertxTestContext ctx) {
    this.helper.testResourceRequestFailsWithNotFound(
        ctx,
        WORKSPACES_PATH + "nonexistent",
        this.client.get(TEST_PORT, TEST_HOST, WORKSPACES_PATH + "nonexistent").send()
    );
  }

  @Test
  public void testGetArtifactSucceeds(final VertxTestContext ctx) {
    this.helper.testGetResourceSucceeds(ctx, "c0_counter_artifact_td.ttl", COUNTER_ARTIFACT_PATH);
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
  public void testGetArtifactFailsWithNotFound(final VertxTestContext ctx) {
    this.helper.testResourceRequestFailsWithNotFound(
        ctx,
        ARTIFACTS_PATH + "nonexistent",
        this.client.get(TEST_PORT, TEST_HOST, ARTIFACTS_PATH + "nonexistent").send()
    );
  }

  @Test
  public void testPostTurtleWorkspacesSucceeds(final VertxTestContext ctx) {
    this.helper.testPostTurtleResourceSucceeds(
        ctx,
        "test_workspace_td.ttl",
        WORKSPACES_PATH,
        MAIN_WORKSPACE_NAME
    );
  }

  @Test
  public void testPostTurtleWorkspacesFailsWithoutWebId(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestFailsWithoutWebId(
          ctx,
          this.client.post(TEST_PORT, TEST_HOST, WORKSPACES_PATH)
                     .putHeader(SLUG_HEADER, MAIN_WORKSPACE_NAME)
                     .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
                     .sendBuffer(Buffer.buffer(Files.readString(Path.of(
                       ClassLoader.getSystemResource("test_workspace_td.ttl").toURI())
                     )))
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostTurtleSubWorkspaceSucceeds(final VertxTestContext ctx) {
    this.helper.testPostTurtleResourceSucceeds(
        ctx,
        "sub_workspace_td.ttl",
        MAIN_WORKSPACE_PATH,
        SUB_WORKSPACE_NAME
    );
  }

  @Test
  public void testPostTurtleSubWorkspaceFailsWithoutWebId(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestFailsWithoutWebId(
          ctx,
          this.client.post(TEST_PORT, TEST_HOST, MAIN_WORKSPACE_PATH)
                     .putHeader(SLUG_HEADER, SUB_WORKSPACE_NAME)
                     .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
                     .sendBuffer(Buffer.buffer(Files.readString(Path.of(
                       ClassLoader.getSystemResource("sub_workspace_td.ttl").toURI())
                     )))
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPostTurtleArtifactSucceeds(final VertxTestContext ctx) {
    this.helper.testPostTurtleResourceSucceeds(
        ctx,
        "c0_counter_artifact_td.ttl",
        ARTIFACTS_PATH,
        COUNTER_ARTIFACT_NAME
    );
  }

  @Test
  public void testPostTurtleArtifactFailsWithoutWebId(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestFailsWithoutWebId(
          ctx,
          this.client.post(TEST_PORT, TEST_HOST, ARTIFACTS_PATH)
                     .putHeader(SLUG_HEADER, SUB_WORKSPACE_NAME)
                     .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
                     .sendBuffer(Buffer.buffer(Files.readString(Path.of(
                       ClassLoader.getSystemResource("sub_workspace_td.ttl").toURI())
                     )))
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPutTurtleWorkspaceSucceeds(final VertxTestContext ctx) {
    this.helper.testPutTurtleResourceSucceeds(

        ctx,
        MAIN_WORKSPACE_PATH,
        "test_workspace_td.ttl"
    );
  }

  @Test
  public void testPutTurtleWorkspaceFailsWithNotFound(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestFailsWithNotFound(
          ctx,
          WORKSPACES_PATH + "nonexistent",
          this.client.put(TEST_PORT, TEST_HOST, WORKSPACES_PATH + "nonexistent")
                     .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                     .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
                     .sendBuffer(Buffer.buffer(Files.readString(Path.of(
                       ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()
                     ))))
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPutTurtleWorkspaceFailsWithoutWebId(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestFailsWithoutWebId(
          ctx,
          this.client.put(TEST_PORT, TEST_HOST, MAIN_WORKSPACE_PATH)
                     .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
                     .sendBuffer(Buffer.buffer(Files.readString(Path.of(
                       ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()
                     ))))
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPutTurtleWorkspaceFailsWithoutContentType(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestFailsWithoutContentType(
          ctx,
          HttpMethod.PUT,
          MAIN_WORKSPACE_PATH,
          Buffer.buffer(Files.readString(Path.of(
            ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()
          )))
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
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
  public void testPutTurtleArtifactSucceeds(final VertxTestContext ctx) {
    this.helper.testPutTurtleResourceSucceeds(
        ctx,
        COUNTER_ARTIFACT_PATH,
        "c0_counter_artifact_td.ttl"
    );
  }

  @Test
  public void testPutTurtleArtifactFailsWithNotFound(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestFailsWithNotFound(
          ctx,
          ARTIFACTS_PATH + "nonexistent",
          this.client.put(TEST_PORT, TEST_HOST, ARTIFACTS_PATH + "nonexistent")
                     .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                     .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
                     .sendBuffer(Buffer.buffer(Files.readString(Path.of(
                       ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()
                     ))))
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPutTurtleArtifactFailsWithoutWebId(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestFailsWithoutWebId(
          ctx,
          this.client.put(TEST_PORT, TEST_HOST, COUNTER_ARTIFACT_PATH)
                     .putHeader(HttpHeaders.CONTENT_TYPE, TURTLE_CONTENT_TYPE)
                     .sendBuffer(Buffer.buffer(Files.readString(Path.of(
                       ClassLoader.getSystemResource("c0_counter_artifact_td.ttl").toURI()
                     ))))
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testPutTurtleArtifactFailsWithoutContentType(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestFailsWithoutContentType(
          ctx,
          HttpMethod.PUT,
          COUNTER_ARTIFACT_PATH,
          Buffer.buffer(Files.readString(Path.of(
            ClassLoader.getSystemResource("c0_counter_artifact_td.ttl").toURI()
          )))
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
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
  public void testDeleteTurtleWorkspaceSucceeds(final VertxTestContext ctx) {
    this.helper.testDeleteTurtleResourceSucceeds(
        ctx,
        MAIN_WORKSPACE_PATH,
        "test_workspace_td.ttl"
    );
  }

  @Test
  public void testDeleteTurtleWorkspaceFailsWithNotFound(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestFailsWithNotFound(
          ctx,
          WORKSPACES_PATH + "nonexistent",
          this.client.delete(TEST_PORT, TEST_HOST, WORKSPACES_PATH + "nonexistent")
                     .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                     .send()
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testDeleteTurtleWorkspaceFailsWithoutWebId(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestFailsWithoutWebId(
          ctx,
          this.client.delete(TEST_PORT, TEST_HOST, MAIN_WORKSPACE_PATH).send()
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
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
  public void testDeleteTurtleArtifactSucceeds(final VertxTestContext ctx) {
    this.helper.testDeleteTurtleResourceSucceeds(
        ctx,
        COUNTER_ARTIFACT_PATH,
        "c0_counter_artifact_td.ttl"
    );
  }

  @Test
  public void testDeleteTurtleArtifactFailsWithNotFound(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestFailsWithNotFound(
          ctx,
          ARTIFACTS_PATH + "nonexistent",
          this.client.delete(TEST_PORT, TEST_HOST, ARTIFACTS_PATH + "nonexistent")
                     .putHeader(AGENT_WEBID, TEST_AGENT_ID)
                     .send()
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }

  @Test
  public void testDeleteTurtleArtifactFailsWithoutWebId(final VertxTestContext ctx) {
    try {
      this.helper.testResourceRequestFailsWithoutWebId(
          ctx,
          this.client.delete(TEST_PORT, TEST_HOST, COUNTER_ARTIFACT_PATH).send()
      );
    } catch (final Exception e) {
      ctx.failNow(e);
    }
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
  public void testEntityCorsHeaders(final VertxTestContext ctx) {
    final var request = this.client.get(TEST_PORT, TEST_HOST, "/").send();
    try {
      this.storeMessageQueue.take().reply(null);
      request
          .onSuccess(r -> {
            Assertions.assertEquals(
                HttpStatus.SC_OK,
                r.statusCode(),
                OK_STATUS_MESSAGE
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
    } catch (final Exception e) {
      ctx.failNow(e);
    }
  }
}
