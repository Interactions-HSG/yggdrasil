package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifact;
import org.hyperagents.yggdrasil.jason.VertxRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * The Annotation Controller Artifact was created for the use case of tackling online disinformation by annotating web resources.
 * It will typically be called from a frontend (a JavaScript web application/browser extension)
 * and then forward the request to an annotator agent on the Yggdrasil platform.
 */
public class AnnotationController extends HypermediaArtifact {
  private List<String> annotationTasks;
  private static Integer AGENT_COUNTER;
  private static final String ANNOTATOR_AGENT_TEMPLATE_PATH = "src/main/java/org/hyperagents/yggdrasil/jason/asl/annotator_agent_template.asl";
  private static final String AGENT_NAME_HEADER = "org.hyperagents.yggdrasil.eventbus.headers.agentName";
  private static final String REQUEST_METHOD = "org.hyperagents.yggdrasil.eventbus.headers.requestMethod";
  public static final String INSTANTIATE_AGENT = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".instantiateAgent";

  public static final String BUS_ADDRESS = "org.hyperagents.yggdrasil.eventbus.jason";
  public void init() {
    AGENT_COUNTER = 0;
    annotationTasks = new ArrayList<>();
  }

  /**
   * Coordinates web annotation requests to be handled by agents.
   * If no agent for the requesting party exists a new agent should be created.
   * // TODO: once the request does accept parameters:
   * OPTION A: The agentURI may be returned to the human agent for a direct interaction between human agent and annotator agent.
   * OPTION B: This controller may route the requests to the corresponding agents by getting the agentURI as input variable
   * This could be taken over by a coordination agent once organizations and agent-to-agent interaction are supported by Yggdrasil.
   */
  // TODO: add annotationURI as input parameter & output as feedback parameter - request does currently not go through with it (09.03.23)
  @OPERATION
  public void annotate(/* TODO: String annotationURI, OpFeedbackParam<String> output */) {
    // TODO: System.out.println("Annotate " + annotationURI);
    String agentURI = null;
    if(AGENT_COUNTER < 10) {
      try {
        agentURI = requestAgentCreation("annotator" + (AGENT_COUNTER + 1), getAnnotatorAgentTemplate())
          .exceptionally(ex -> null)
          .get();
      } catch (InterruptedException | ExecutionException | IOException e) {
        failed(e.getMessage());
      }
      ++AGENT_COUNTER;
      System.out.println("Created agent");
      // TODO: output.set(agentURI)
    } else {
      System.out.println("Maximum number of agents is reached!");
    }
  }

  /**
   * Gets the asl annotator agent template file
   * @return content of the template file as a string
   * @throws IOException exception when reading the file input
   */
  private String getAnnotatorAgentTemplate() throws IOException {
    Path path = Paths.get(ANNOTATOR_AGENT_TEMPLATE_PATH);
    return Files.readString(path);
  }

  /**
   * @param agentName     name of the agent to be created
   * @param agentTemplate the asl file content of the agent to be created
   * @return completable future of type String that should return the URI of the created agent
   */
  private CompletableFuture<String> requestAgentCreation(String agentName, String agentTemplate) {
    CompletableFuture<String> cf = new CompletableFuture<>();

    Vertx vertx = VertxRegistry.getInstance().getVertx();
    EventBus eventBus = vertx.eventBus();
    DeliveryOptions deliveryOptions = new DeliveryOptions();
    deliveryOptions.addHeader(REQUEST_METHOD, INSTANTIATE_AGENT);
    deliveryOptions.addHeader(AGENT_NAME_HEADER, agentName);

    eventBus.request(BUS_ADDRESS, agentTemplate, deliveryOptions, reply -> {
      if (reply.succeeded()) {
        System.out.println("Received reply " + reply.result().body());
        cf.complete(reply.result().body().toString());
      } else {
        System.out.println("Agent Creation failed: " + reply.cause().getMessage());
        cf.completeExceptionally(reply.cause());
      }
    });

    return cf;
  }

  @Override
  protected void registerInteractionAffordances() {
    // Register one action affordance with an input schema
    StringSchema schema = new StringSchema.Builder().build();
    registerActionAffordance("http://example.org/annotate", "annotate", "/annotate", schema);
    // TODO: registerFeedbackParameter("annotate");
  }

  public List<String> getAnnotationTasks() {
    return annotationTasks;
  }
}
