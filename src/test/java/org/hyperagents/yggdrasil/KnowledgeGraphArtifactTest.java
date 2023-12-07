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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.hyperagents.yggdrasil.cartago.CartagoVerticle;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.store.RdfStoreVerticle;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;
import org.hyperagents.yggdrasil.utils.impl.RepresentationFactoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@ExtendWith(VertxExtension.class)
public class KnowledgeGraphArtifactTest {
  private static final String TEST_AGENT_NAME = "test";
  private static final String RESULTS_EQUAL_MESSAGE = "The results should be equal";
  private static final String KNOWLEDGE_GRAPH_ARTIFACT_NAME = "knowledge_graph";
  private static final String TEST_WORKSPACE_NAME = "test";

  private final List<Promise<Object>> completionPromises;
  private final ICartagoContext yggdrasilWorkspace;

  @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
  public KnowledgeGraphArtifactTest() throws CartagoException {
    this.completionPromises = Stream.generate(Promise::promise)
                                    .limit(5)
                                    .toList();
    final var environment = CartagoEnvironment.getInstance();
    environment.init(new BasicLogger());
    this.yggdrasilWorkspace = environment.getRootWSP().getWorkspace().joinWorkspace(
      new AgentIdCredential(TEST_AGENT_NAME),
      e -> {
        if (
            e instanceof ActionSucceededEvent s
            && Optional.ofNullable(s.getOp())
                       .map(Op::getName)
                       .map(Set.of("ask", "select", "selectOne")::contains)
                       .orElse(false)
        ) {
          this.completionPromises
              .get((int) s.getActionId())
              .complete(((OpFeedbackParam<?>) s.getOp().getParamValues()[1]).get());
        } else if (e instanceof ActionFailedEvent f) {
          this.completionPromises
              .get((int) f.getActionId())
              .fail(f.getFailureMsg());
        }
      }
    );
  }

  @BeforeEach
  public void setUp(final Vertx vertx, final VertxTestContext ctx) {
    final var messagebox = new RdfStoreMessagebox(vertx.eventBus());
    final var httpConfig = new HttpInterfaceConfigImpl(JsonObject.of());
    final var representationFactory = new RepresentationFactoryImpl(httpConfig);
    final var notificationMessagebox = new HttpNotificationDispatcherMessagebox(vertx.eventBus());
    notificationMessagebox.init();
    vertx.deployVerticle(new CartagoVerticle())
         .compose(i -> vertx.deployVerticle(new RdfStoreVerticle()))
         .compose(i -> messagebox.sendMessage(new RdfStoreMessage.CreateWorkspace(
           httpConfig.getWorkspacesUri() + "/",
           TEST_WORKSPACE_NAME,
           Optional.empty(),
           representationFactory.createWorkspaceRepresentation(TEST_WORKSPACE_NAME, Set.of())
         )))
         .compose(r -> messagebox.sendMessage(new RdfStoreMessage.CreateWorkspace(
           httpConfig.getWorkspacesUri() + "/",
           "sub",
           Optional.of(httpConfig.getWorkspaceUri(TEST_WORKSPACE_NAME)),
           representationFactory.createWorkspaceRepresentation("sub", Set.of())
         )))
         .onComplete(ctx.succeedingThenComplete());
  }

  @AfterEach
  public void tearDown(final Vertx vertx, final VertxTestContext ctx) {
    vertx.close(ctx.succeedingThenComplete());
  }

  @Test
  public void testAskQuery(final VertxTestContext ctx) throws CartagoException {
    this.completionPromises
        .get(0)
        .future()
        .onSuccess(r -> Assertions.assertTrue(
          (Boolean) r,
          RESULTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
    this.yggdrasilWorkspace
        .doAction(0,
                  KNOWLEDGE_GRAPH_ARTIFACT_NAME,
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
                  -1);
  }

  @Test
  public void testSelectQuery(final VertxTestContext ctx) throws CartagoException {
    this.completionPromises
        .get(1)
        .future()
        .onSuccess(r -> Assertions.assertArrayEquals(
          new String[][][] {
            new String[][]{
              new String[]{"name", TEST_WORKSPACE_NAME},
              new String[]{"uri", "http://localhost:8080/workspaces/test"}
            },
            new String[][]{
              new String[]{"name", "sub"},
              new String[]{"uri", "http://localhost:8080/workspaces/sub"}
            }
          },
          (String[][][]) r,
          RESULTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
    this.yggdrasilWorkspace
        .doAction(1,
                  KNOWLEDGE_GRAPH_ARTIFACT_NAME,
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
                  -1);
  }

  @Test
  public void testSelectQueryWithNoResult(final VertxTestContext ctx) throws CartagoException {
    this.completionPromises
        .get(2)
        .future()
        .onSuccess(r -> Assertions.assertArrayEquals(
          new String[][][] {},
          (String[][][]) r,
          RESULTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
    this.yggdrasilWorkspace
        .doAction(2,
                  KNOWLEDGE_GRAPH_ARTIFACT_NAME,
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
                  -1);
  }

  @Test
  public void testSelectOneQuery(final VertxTestContext ctx) throws CartagoException {
    this.completionPromises
        .get(3)
        .future()
        .onSuccess(r -> Assertions.assertArrayEquals(
          new String[][]{
            new String[]{"uri", "http://localhost:8080/workspaces/sub"}
          },
          (String[][]) r,
          RESULTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
    this.yggdrasilWorkspace
        .doAction(3,
                  KNOWLEDGE_GRAPH_ARTIFACT_NAME,
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
                  -1);
  }

  @Test
  public void testSelectOneQueryWithNoResult(final VertxTestContext ctx) throws CartagoException {
    this.completionPromises
        .get(4)
        .future()
        .onSuccess(r -> Assertions.assertNull(
          r,
          "The result should be null"
        ))
        .onComplete(ctx.succeedingThenComplete());
    this.yggdrasilWorkspace
        .doAction(4,
                  KNOWLEDGE_GRAPH_ARTIFACT_NAME,
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
                  -1);
  }
}
