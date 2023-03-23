package org.hyperagents.yggdrasil.coap;

import com.mbed.coap.exception.CoapException;
import com.mbed.coap.server.CoapExchange;
import com.mbed.coap.server.CoapHandler;
import com.mbed.coap.server.CoapServer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

public class CoapServerVerticle extends AbstractVerticle {

  @Override
  public void start(){

    JsonObject config = config();

    CoapServer server = CoapServer.builder().transport(5683).build(); //TODO: get port from config

    try {
      server.start();
    } catch (Exception e){
      e.printStackTrace();
    }
  }
}
