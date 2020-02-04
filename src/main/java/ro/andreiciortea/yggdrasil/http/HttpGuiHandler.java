package ro.andreiciortea.yggdrasil.http;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.Router;


import java.util.HashSet;
import java.util.Set;

public class HttpGuiHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpTemplateHandler.class.getName());
  private Vertx vertx;
  private Set<ServerWebSocket> websockets = new HashSet<>();


  public HttpGuiHandler(HttpServer server, Router router) {
    vertx = Vertx.currentContext().owner();

    server.websocketHandler(ws-> {
        websockets.add(ws);
        ws.handler(buffer -> {
          for(ServerWebSocket other : websockets) {
            if(other != ws) {
              other.writeTextMessage(buffer.toString());
            }
          }
        });
        ws.closeHandler(h-> {
          System.out.println("Closing handler, removing ws from websockets");
          websockets.remove(ws);
        });
      });

      router.route("/gui/*").handler(StaticHandler.create());  // serves static react app from src/main/resources/webroot folder
  }
}
