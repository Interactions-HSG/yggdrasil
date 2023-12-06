package org.hyperagents.yggdrasil;

import cartago.AgentIdCredential;
import cartago.CartagoEnvironment;
import cartago.CartagoException;
import cartago.ICartagoContext;
import cartago.Op;
import cartago.OpFeedbackParam;
import cartago.events.ActionFailedEvent;
import cartago.events.ActionSucceededEvent;
import cartago.utils.BasicLogger;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class KnowledgeGraphArtifactTest {
  private static final String TEST_AGENT_NAME = "test";
  private static final String TEST_HOST = "localhost";
  private static final int TEST_PORT = 8080;
  private static final int CARTAGO_PORT = 8088;

  private final Process yggdrasilProcess;
  private final List<Promise<Object>> completionPromises;
  private final ICartagoContext yggdrasilWorkspace;
  private int completionsCount;

  public KnowledgeGraphArtifactTest() throws IOException, CartagoException {
    this.completionPromises = Stream.generate(Promise::promise)
                                    .limit(5)
                                    .toList();
    this.completionsCount = 0;
    final var processBuilder = new ProcessBuilder();
    processBuilder.environment().put("VERTX_CONFIG_PATH", "src/test/resources/config.json");
    this.yggdrasilProcess = processBuilder.command("./gradlew", "run", "--no-daemon")
                                          .inheritIO()
                                          .start();
    var started = false;
    do {
      try (var ignored = SocketChannel.open(new InetSocketAddress(TEST_HOST, CARTAGO_PORT))) {
        started = true;
      } catch (final Exception ignored) {}
    } while (!started);
    final var environment = CartagoEnvironment.getInstance();
    environment.init(new BasicLogger());
    environment.installInfrastructureLayer("web");
    this.yggdrasilWorkspace = environment.joinRemoteWorkspace(
      "",
      TEST_HOST + ":" + CARTAGO_PORT,
      "/main",
      "web",
      new AgentIdCredential(TEST_AGENT_NAME),
      e -> {
        if (e instanceof ActionSucceededEvent s) {
          this.completionPromises
              .get(this.completionsCount)
              .complete(((OpFeedbackParam<?>) s.getOp().getParamValues()[1]).get());
        } else if (e instanceof ActionFailedEvent f) {
          this.completionPromises
              .get(this.completionsCount)
              .fail(f.getFailureMsg());
        }
        this.completionsCount++;
      },
      "yggdrasil"
    );
  }

  @BeforeAll
  public void beforeAll(final Vertx vertx, final VertxTestContext ctx) {
    final WebClient client = WebClient.create(vertx);
    client.post(TEST_PORT, TEST_HOST, "/workspaces/")
          .putHeader("Slug", "test")
          .putHeader("X-Agent-WebID", TEST_AGENT_NAME)
          .send()
          .compose(r -> client.post(TEST_PORT, TEST_HOST, "/workspaces/test")
                              .putHeader("Slug", "sub")
                              .putHeader("X-Agent-WebID", TEST_AGENT_NAME)
                              .send())
          .onComplete(ctx.succeedingThenComplete());
  }

  @AfterAll
  public void afterAll(final Vertx vertx, final VertxTestContext ctx) {
    this.yggdrasilProcess.onExit().thenRun(() -> vertx.close(ctx.succeedingThenComplete()));
    this.yggdrasilProcess.destroy();
  }

  @Test
  public void testAskQuery(final VertxTestContext ctx) throws CartagoException {
    this.completionPromises
        .get(this.completionsCount)
        .future()
        .onSuccess(r -> Assertions.assertEquals(
          true,
          r,
          "The results should be equal"
        ))
        .onComplete(ctx.succeedingThenComplete());
    this.yggdrasilWorkspace
        .doAction(
          0,
          "knowledge_graph",
          new Op(
            "ask",
            """
              PREFIX td: <https://www.w3.org/2019/wot/td#>
              PREFIX hmas: <https://purl.org/hmas/core/>

              ASK WHERE {
                  [] hmas:contains [
                      a hmas:Workspace;
                      td:title "sub";
                  ];
                  a hmas:Workspace.
              }
               """,
            new OpFeedbackParam<Boolean>()
          ),
          null,
          -1
        );
  }

  @Test
  public void testSelectQuery(final VertxTestContext ctx) throws CartagoException {
    this.completionPromises
        .get(this.completionsCount)
        .future()
        .onSuccess(r -> Assertions.assertArrayEquals(
          new String[][][] {
            new String[][]{
              new String[]{"name", "test"},
              new String[]{"uri", "http://localhost:8080/workspaces/test"}
            },
            new String[][]{
              new String[]{"name", "sub"},
              new String[]{"uri", "http://localhost:8080/workspaces/sub"}
            }
          },
          (String[][][]) r,
          "The results should be equal"
        ))
        .onComplete(ctx.succeedingThenComplete());
    this.yggdrasilWorkspace
        .doAction(
          0,
          "knowledge_graph",
          new Op(
            "select",
            """
              PREFIX td: <https://www.w3.org/2019/wot/td#>
              PREFIX hmas: <https://purl.org/hmas/core/>

              SELECT DISTINCT ?name ?uri
              WHERE {
                  ?uri a hmas:Workspace;
                       td:title ?name.
              }
               """,
            new OpFeedbackParam<String[][][]>()
          ),
          null,
          -1
        );
  }

  @Test
  public void testSelectQueryWithNoResult(final VertxTestContext ctx) throws CartagoException {
    this.completionPromises
        .get(this.completionsCount)
        .future()
        .onSuccess(r -> Assertions.assertArrayEquals(
          new String[][][] {},
          (String[][][]) r,
          "The results should be equal"
        ))
        .onComplete(ctx.succeedingThenComplete());
    this.yggdrasilWorkspace
        .doAction(
          0,
          "knowledge_graph",
          new Op(
            "select",
            """
              PREFIX td: <https://www.w3.org/2019/wot/td#>
              PREFIX hmas: <https://purl.org/hmas/core/>

              SELECT DISTINCT ?name ?uri
              WHERE {
                  ?uri a hmas:Workspace;
                       td:title "nonexistent".
              }
               """,
            new OpFeedbackParam<String[][][]>()
          ),
          null,
          -1
        );
  }

  @Test
  public void testSelectOneQuery(final VertxTestContext ctx) throws CartagoException {
    this.completionPromises
        .get(this.completionsCount)
        .future()
        .onSuccess(r -> Assertions.assertArrayEquals(
          new String[][]{
            new String[]{"uri", "http://localhost:8080/workspaces/sub"}
          },
          (String[][]) r,
          "The results should be equal"
        ))
        .onComplete(ctx.succeedingThenComplete());
    this.yggdrasilWorkspace
        .doAction(
          0,
          "knowledge_graph",
          new Op(
            "selectOne",
            """
              PREFIX td: <https://www.w3.org/2019/wot/td#>
              PREFIX hmas: <https://purl.org/hmas/core/>

              SELECT DISTINCT ?uri
              WHERE {
                  ?uri a hmas:Workspace;
                       td:title "sub".
              }
               """,
            new OpFeedbackParam<String[][]>()
          ),
          null,
          -1
        );
  }

  @Test
  public void testSelectOneQueryWithNoResult(final VertxTestContext ctx) throws CartagoException {
    this.completionPromises
        .get(this.completionsCount)
        .future()
        .onSuccess(r -> Assertions.assertNull(
          r,
          "The result should be null"
        ))
        .onComplete(ctx.succeedingThenComplete());
    this.yggdrasilWorkspace
        .doAction(
          0,
          "knowledge_graph",
          new Op(
            "selectOne",
            """
              PREFIX td: <https://www.w3.org/2019/wot/td#>
              PREFIX hmas: <https://purl.org/hmas/core/>

              SELECT DISTINCT ?uri
              WHERE {
                  ?uri a hmas:Workspace;
                       td:title "nonexistent".
              }
               """,
            new OpFeedbackParam<String[][]>()
          ),
          null,
          -1
        );
  }
}
