package org.hyperagents.yggdrasil.http;

import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.websub.HttpNotificationVerticle;

import com.google.common.net.HttpHeaders;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/** 
 * This verticle exposes an HTTP/1.1 interface for Yggdrasil. All requests are forwarded to a 
 * corresponding handler.
 */
public class HttpServerVerticle extends AbstractVerticle {
  public static final String DEFAULT_HOST = "0.0.0.0";
  public static final int DEFAULT_PORT = 8080;
  
  /* Keys used when parsing a JSON config file to extract HTTP settings */
  public static final String CONFIG_HTTP = "http-config";
  public static final String CONFIG_HTTP_PORT = "port";
  public static final String CONFIG_HTTP_HOST = "host";
  
  @Override
  public void start() {
    HttpServer server = vertx.createHttpServer();

    int port = DEFAULT_PORT;
    String host = DEFAULT_HOST;
    JsonObject httpConfig = config().getJsonObject(CONFIG_HTTP);
    
    if (httpConfig != null) {
      port = httpConfig.getInteger(CONFIG_HTTP_PORT, DEFAULT_PORT);
      host = httpConfig.getString(CONFIG_HTTP_HOST, DEFAULT_HOST);
    }
    
    Router router = createRouter();
    server.requestHandler(router).listen(port, host);
  }
  
  /**
   * The HTTP API is defined here when creating the router.
   */
  private Router createRouter() {
    Router router = Router.router(vertx);
    
    router.route().handler(BodyHandler.create());
    
    router.get("/").handler((routingContext) -> {
      routingContext.response()
        .setStatusCode(HttpStatus.SC_OK)
        .end("Yggdrasil v0.0");
    });
    
    HttpEntityHandler handler = new HttpEntityHandler(vertx);
    
    router.get("/environments/:envid").handler(handler::handleGetEntity);
//    router.post("/environments/").handler(handler::handleCreateEntity);
    router.post("/environments/").handler(handler::handleCreateEnvironment);
    router.put("/environments/:envid").handler(handler::handleUpdateEntity);
    router.delete("/environments/:envid").handler(handler::handleDeleteEntity);
    
    router.get("/environments/:envid/workspaces/:wkspid").handler(handler::handleGetEntity);
    router.post("/environments/:envid/workspaces/").handler(handler::handleCreateWorkspace);
    router.put("/environments/:envid/workspaces/:wkspid").handler(handler::handleUpdateEntity);
    router.delete("/environments/:envid/workspaces/:wkspid").handler(handler::handleDeleteEntity);
    
    router.get("/environments/:envid/workspaces/:wkspid/artifacts/:artid").handler(handler::handleGetEntity);
    router.post("/environments/:envid/workspaces/:wkspid/artifacts/").handler(handler::handleCreateArtifact);
    router.put("/environments/:envid/workspaces/:wkspid/artifacts/:artid").handler(handler::handleUpdateEntity);
    router.delete("/environments/:envid/workspaces/:wkspid/artifacts/:artid").handler(handler::handleDeleteEntity);
    router.route("/environments/:envid/workspaces/:wkspid/artifacts/:artid/*").handler(handler::handleAction);
    
    //route artifact manual requests
    router.get("/manuals/:wkspid").handler(handler::handleGetEntity);
    router.post("/manuals/").handler(handler::handleCreateEntity);
    router.put("/manuals/:wkspid").handler(handler::handleUpdateEntity);
    router.delete("/manuals/:wkspid").handler(handler::handleDeleteEntity);

    router.post("/hub/").handler(handler::handleEntitySubscription);

    // TODO: the following feature is added just for demo purposes
    router.post("/events/").handler(routingContext -> {
      String artifactIRI = routingContext.request().getHeader(HttpHeaders.LINK);

      Logger logger = LoggerFactory.getLogger(this.getClass().getName());
      logger.info("Got event for " + artifactIRI);

      DeliveryOptions options = new DeliveryOptions()
          .addHeader(HttpEntityHandler.REQUEST_METHOD, HttpNotificationVerticle
              .ENTITY_CHANGED)
          .addHeader(HttpEntityHandler.REQUEST_URI, artifactIRI);
      
      vertx.eventBus().send(HttpNotificationVerticle.BUS_ADDRESS, 
          routingContext.getBodyAsString(), options);
      
      routingContext.response().setStatusCode(HttpStatus.SC_OK).end();
    });
    
    return router;
  }
}
