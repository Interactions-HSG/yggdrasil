package org.hyperagents.yggdrasil.jason;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.apache.http.HttpStatus;
import org.apache.tools.ant.taskdefs.condition.Http;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.hyperagents.yggdrasil.cartago.CartagoVerticle;
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

  public static final String DELETE_AGENT = "org.hyperagents.yggdrasil.eventbus.headers.methods"
    + ".deleteAgent";

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

  private final static ValueFactory rdf = SimpleValueFactory.getInstance();

  @Override
  public void start(){
    LOGGER.info("start Jason Verticle");
    agentService = new YggdrasilRuntimeServices(new RunYggdrasilMAS());
    VertxRegistry.getInstance().setVertx(this.vertx);
    EventBus eventBus = vertx.eventBus();
    eventBus.consumer(BUS_ADDRESS, this::handleAgentRequest);
    HttpInterfaceConfig httpConfig = new HttpInterfaceConfig(config()); //TODO: see if needed, maybe for URIs
    JsonObject config = config();
    System.out.println("config: "+config.encodePrettily());
    AgentRegistry.getInstance().setHttpPrefix(config);
  }

  private void handleAgentRequest(Message<String> message){
    String requestMethod = message.headers().get(HttpEntityHandler.REQUEST_METHOD);
    String agentName = message.headers().get(AGENT_NAME);
    switch(requestMethod){
      case INSTANTIATE_AGENT:
        System.out.println("instantiate agent");
        String aslFile = message.body();
        String agentUrl = instantiateAgent(agentName, aslFile);
        System.out.println("agent url: "+ agentUrl);
        message.reply(agentUrl);
        break;
      case DELETE_AGENT:
        deleteAgent(agentName);
        message.reply("agent deleted");


    }

  }

  private String instantiateAgent(String agentName, String aslFile){
    System.out.println("start instantiate agent");
    String hypermediaAgentName = "";
    try {
      hypermediaAgentName = AgentRegistry.getInstance().addAgent(agentName);
      System.out.println("hypermedia agent name 1: "+hypermediaAgentName);
      InputStream stream = new ByteArrayInputStream(aslFile.getBytes());
      String agArchName = agentService.createAgent(hypermediaAgentName, stream, hypermediaAgentName);
      System.out.println("before start agent");
      agentService.startAgent(agArchName);
      System.out.println("after start agent");
      hypermediaAgentName = AgentRegistry.getInstance().getAgentUri(agentName);
      System.out.println("hypermedia agent name: "+hypermediaAgentName);
      IRI agentIRI = rdf.createIRI(hypermediaAgentName);
      System.out.println("agent IRI: "+agentIRI);
      System.out.println("before register agent");
      registerAgent(agentName, agentIRI);
      System.out.println("after register agent");
    } catch(Exception e){
      e.printStackTrace();
    }
    return hypermediaAgentName;
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

  private void deleteAgent(String agentName){
    System.out.println("delete agent: "+ agentName);
    System.out.println("agent killed");
    try {
      String agentUri = AgentRegistry.getInstance().getAgentUri(agentName);
      agentService.killAgent(agentName, "", 5); //TODO: check parameters
      AgentRegistry.getInstance().deleteAgent(agentName);
      System.out.println("agent deleted");
      DeliveryOptions options = new DeliveryOptions()
        .addHeader(HttpEntityHandler.REQUEST_METHOD, RdfStore.DELETE_ENTITY)
        .addHeader(HttpEntityHandler.REQUEST_URI, agentUri);

      vertx.eventBus().request(RdfStore.BUS_ADDRESS, null, options,
        reply -> System.out.println(reply));
    } catch (Exception e){
      e.printStackTrace();
    }
  }





}
