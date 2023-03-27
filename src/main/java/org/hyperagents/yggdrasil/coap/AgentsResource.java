package org.hyperagents.yggdrasil.coap;

import com.mbed.coap.exception.CoapCodeException;
import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.packet.Code;
import com.mbed.coap.server.CoapExchange;
import com.mbed.coap.utils.CoapResource;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.ext.web.RoutingContext;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;
import org.hyperagents.yggdrasil.jason.AgentRegistry;
import org.hyperagents.yggdrasil.jason.JasonVerticle;

import java.util.Arrays;

public class AgentsResource extends CoapResource {

  Vertx vertx;

  public AgentsResource(Vertx vertx){
    this.vertx = vertx;
  }
  @Override
  public void get(CoapExchange coapExchange) throws CoapCodeException {
    coapExchange.setResponseCode(Code.C405_METHOD_NOT_ALLOWED);
    coapExchange.sendResponse();
  }

  @Override
  public void post(CoapExchange coapExchange) throws CoapCodeException {
   handleInstantiateAgent(coapExchange);
  }

  public void handleInstantiateAgent(CoapExchange coapExchange){
    System.out.println("handle instantiate agent");
    String agentId = Arrays.toString(coapExchange.getRequestHeaders().getCustomOption(2048));
    String agentName = Arrays.toString(coapExchange.getRequestHeaders().getCustomOption(2049));
    System.out.println("agent name: "+ agentName);

    String representation = coapExchange.getRequestBodyString();
    System.out.println("representation: "+representation);
    if (agentId == null){
      coapExchange.setResponseCode(Code.C401_UNAUTHORIZED);
    }
    DeliveryOptions options = new DeliveryOptions()
      .addHeader(HttpEntityHandler.REQUEST_METHOD, JasonVerticle.INSTANTIATE_AGENT)
      .addHeader(JasonVerticle.AGENT_NAME, agentName);

    vertx.eventBus().request(JasonVerticle.BUS_ADDRESS, representation, options, reply -> {
      if (reply.succeeded()) {
        AgentRegistry.getInstance().printAllAgents();
        System.out.println("agent creation succeeded");
        coapExchange.setResponseCode(Code.C201_CREATED);
        coapExchange.setResponseBody(reply.result().body().toString());
        coapExchange.sendResponse();
      } else {
        System.out.println("agent creation failed");
        coapExchange.setResponseCode(Code.C500_INTERNAL_SERVER_ERROR);
        coapExchange.sendResponse();
      }
    });
  }
}
