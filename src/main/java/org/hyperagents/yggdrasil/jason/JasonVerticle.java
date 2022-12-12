package org.hyperagents.yggdrasil.jason;

import cartago.util.agent.Agent;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.hyperagents.yggdrasil.cartago.CartagoVerticle;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifactRegistry;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;
import org.hyperagents.yggdrasil.http.HttpInterfaceConfig;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.hyperagents.yggdrasil.store.RdfStore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class JasonVerticle extends AbstractVerticle {

  public static final String BUS_ADDRESS = "org.hyperagents.yggdrasil.eventbus.jason";

  public static final String AGENT_ID = "org.hyperagents.yggdrasil.eventbus.headers.agentID";

  public static final String AGENT_NAME = "org.hyperagents.yggdrasil.eventbus.headers.agentName";

  public static final String CREATE_AGENT = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".createAgent";

  public static final String INSTANTIATE_AGENT = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".instantiateAgent";

  public static final String CREATE_AGENT_FROM_FILE_NAME = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".createAgentFromFileName";

  public static final String INSTANTIATE_AGENT_FROM_FILE_NAME = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".instantiateAgentFromFileName";

  public static final String ADD_ASL_FILE = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".addASLFile";

  public static final String FILE_NAME = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".fileName";

  private YggdrasilRuntimeServices agentService;

  private static final Logger LOGGER = LoggerFactory.getLogger(CartagoVerticle.class.getName());

  private static ValueFactory rdf = SimpleValueFactory.getInstance();

  @Override
  public void start(){
    LOGGER.info("start Jason Verticle");
    agentService = new YggdrasilRuntimeServices(new RunYggdrasilMAS());
    VertxRegistry.getInstance().setVertx(this.vertx);
    EventBus eventBus = vertx.eventBus();
    eventBus.consumer(BUS_ADDRESS, this::handleAgentRequest);
    HttpInterfaceConfig httpConfig = new HttpInterfaceConfig(config());
  }

  private void handleAgentRequest(Message<String> message){
    String requestMethod = message.headers().get(HttpEntityHandler.REQUEST_METHOD);
    String agentName = message.headers().get(AGENT_NAME);
    switch(requestMethod){
      case INSTANTIATE_AGENT:
        String aslFile = message.body();
        instantiateAgent(agentName, aslFile);
        message.reply("agent created");
        break;


    }

  }

  private void instantiateAgent(String agentName, String aslFile){
    try {
      String hypermediaAgentName = AgentRegistry.getInstance().addAgent(agentName);
      InputStream stream = new ByteArrayInputStream(aslFile.getBytes());
      String agArchName = agentService.createAgent(hypermediaAgentName, stream, hypermediaAgentName);
      agentService.startAgent(agArchName);
      hypermediaAgentName = AgentRegistry.getInstance().getAgentUri(agentName);
      System.out.println("hypermedia agent name: "+hypermediaAgentName);
      IRI agentIRI = rdf.createIRI(hypermediaAgentName);
      System.out.println("agent IRI: "+agentIRI);
      registerAgent(agentName, agentIRI);
    } catch(Exception e){
      e.printStackTrace();
    }
  }

  private void registerAgent(String agentName, IRI agentIRI){
    System.out.println("register agent");
    String agentDescription = generateDefaultAgentDescription(agentIRI);
    System.out.println("agent description: "+agentDescription);
    //String artifactDescription = HypermediaArtifactRegistry.getInstance().getArtifactDescription(artifactName);
    DeliveryOptions options = new DeliveryOptions()
      .addHeader("org.hyperagents.yggdrasil.eventbus.headers.requestMethod", RdfStore.CREATE_ENTITY)
      .addHeader("org.hyperagents.yggdrasil.eventbus.headers.requestUri", AgentRegistry.getInstance().getHttpPrefix() + "agents/")
      .addHeader("org.hyperagents.yggdrasil.eventbus.headers.slug", agentName);
    vertx.eventBus().request(RdfStore.BUS_ADDRESS, agentDescription, options, result -> {
      if (result.succeeded()) {
        System.out.println("agent stored");
      } else {
        System.out.println("agent could not be stored");
      }
    });
  }

  private String generateDefaultAgentDescription(IRI agentIRI){
    ModelBuilder builder = new ModelBuilder();
    builder.add(agentIRI, rdf.createIRI("http://example.org/hasName"), agentIRI);
    Model model = builder.build();
    OutputStream out = new ByteArrayOutputStream();
    Rio.write(model, out, RDFFormat.TURTLE);
    String modelString = out.toString();
    return modelString;

  }





}
