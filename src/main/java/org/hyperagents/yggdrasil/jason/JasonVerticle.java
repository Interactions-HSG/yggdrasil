package org.hyperagents.yggdrasil.jason;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

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

  @Override
  public void start(){
    agentService = new YggdrasilRuntimeServices(new RunYggdrasilMAS());
    VertxRegistry.getInstance().setVertx(this.vertx);
    EventBus eventBus = vertx.eventBus();
    eventBus.consumer(BUS_ADDRESS, this::handleAgentRequest);
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
    } catch(Exception e){
      e.printStackTrace();
    }
  }





}
