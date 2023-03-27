package org.hyperagents.yggdrasil.coap;

import com.mbed.coap.exception.CoapException;
import com.mbed.coap.packet.CoapPacket;
import com.mbed.coap.server.CoapExchange;
import com.mbed.coap.server.CoapHandler;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.server.CoapTcpCSMStorage;
import com.mbed.coap.server.internal.CoapMessaging;
import com.mbed.coap.server.internal.CoapTcpCSMStorageImpl;
import com.mbed.coap.server.internal.CoapTcpMessaging;
import com.mbed.coap.server.internal.CoapUdpMessaging;
import com.mbed.coap.transport.CoapReceiver;
import com.mbed.coap.transport.CoapTransport;
import com.mbed.coap.transport.InMemoryCoapTransport;
import com.mbed.coap.transport.TransportContext;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class CoapServerVerticle extends AbstractVerticle {

  @Override
  public void start(){

    JsonObject config = config();

    int port = 5683;

    Executor executor = new Executor() {
      @Override
      public void execute(@NotNull Runnable command) {
        command.run();
      }
    };
    CoapTransport transport = new InMemoryCoapTransport(port, executor);


    CoapTcpCSMStorage csmStorage = new CoapTcpCSMStorageImpl();

    //CoapMessaging messaging = new CoapTcpMessaging(transport, csmStorage, true, 1024);

    CoapMessaging messaging = new CoapUdpMessaging(transport);

    CoapServer server = new CoapServer(messaging);

    server.addRequestHandler("/", new DefaultResource());

    server.addRequestHandler("/agents/", new AgentsResource(vertx));

    try {
      server.start();
    } catch (Exception e){
      e.printStackTrace();
    }
  }
}
