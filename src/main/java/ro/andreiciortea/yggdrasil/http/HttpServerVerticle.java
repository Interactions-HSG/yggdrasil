package ro.andreiciortea.yggdrasil.http;

import org.apache.http.HttpStatus;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class HttpServerVerticle extends AbstractVerticle {

  public static final String DEFAULT_HOST = "localhost";
  public static final int DEFAULT_PORT = 8080;
  
  private final static Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class.getCanonicalName());
  
  @Override
  public void start() {
    HttpServer server = vertx.createHttpServer();
    
    String host = DEFAULT_HOST;
    int port = DEFAULT_PORT;
    JsonObject httpConfig = config().getJsonObject("http-config");
    
    if (httpConfig != null) {
      port = httpConfig.getInteger("port", DEFAULT_PORT);
      host = httpConfig.getString("host", DEFAULT_HOST);
    }
    
    Router router = createRouter();
    server.requestHandler(router::accept).listen(port, host);        
  }
  
  private Router createRouter() {
    Router router = Router.router(vertx);
    
    router.route().handler(BodyHandler.create());
    
    router.get("/").handler((routingContext) -> {
      routingContext.response()
        .setStatusCode(HttpStatus.SC_OK)
        .end("Hello Vert.x!");
    });
    
    
    HttpEntityHandler handler = new HttpEntityHandler();
    
    router.post("/environments/").handler(handler::handleCreateEntity);
    router.get("/environments/:envid").handler(handler::handleGetEntity);
//    router.delete("/environments/:envid").handler(handler::handleDeleteEntity);

    router.post("/environments/:envid/workspaces/").handler(handler::handleCreateEntity);
    router.get("/environments/:envid/workspaces/:wkspid").handler(handler::handleGetEntity);
    
//    router.get("/environments/:envid/workspaces/:wkspid").handler((routingContext) -> {
//      String requestUri = routingContext.request().absoluteURI();
//      routingContext.response().setStatusCode(HttpStatus.SC_OK).end("Workspace URI: " + requestUri);
//    });
    
    return router;
  }
}
