package org.hyperagents.yggdrasil.coap;

import com.mbed.coap.client.CoapClient;
import com.mbed.coap.client.CoapClientBuilder;
import com.mbed.coap.packet.CoapPacket;
import io.vertx.core.AbstractVerticle;

import java.net.InetSocketAddress;

public class CoapClientVerticle extends AbstractVerticle {

  @Override
  public void start(){
    try {
      CoapClient client = CoapClientBuilder.newBuilder(new InetSocketAddress("localhost", 5683)).build();
      CoapPacket p = client.resource("/").sync().get();
      String payloadString = p.getPayloadString();
      System.out.println("coap payload received: "+ payloadString);
    } catch (Exception e){
      e.printStackTrace();
    }
    }


}
