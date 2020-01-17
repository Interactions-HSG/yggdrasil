package ro.andreiciortea.yggdrasil.http;

import com.google.common.net.HttpHeaders;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.core.http.HttpMethod;


import org.apache.http.HttpStatus;
import ro.andreiciortea.yggdrasil.core.EventBusMessage;
import ro.andreiciortea.yggdrasil.core.EventBusMessage.Headers;
import ro.andreiciortea.yggdrasil.core.EventBusMessage.MessageType;
import ro.andreiciortea.yggdrasil.core.EventBusRegistry;

import java.util.HashSet;
import java.util.Set;


/*
 * The main entrypoint of Yggdrasil. All requests arrive here and are forwarded to the corresponding handler.
 */
public class HttpServerVerticle extends AbstractVerticle {

  public static final String DEFAULT_HOST = "0.0.0.0";
  public static final int DEFAULT_PORT = 8080;

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
    addGuiRoutesAndWebsocket(server, router);
    server.requestHandler(router::accept).listen(port, host, res->{
      if(res.succeeded()){
          System.out.println("Successfully started http and websocket server");
      } else {
          System.out.println(res.cause().getLocalizedMessage());
      }
    });
  }

  private void addGuiRoutesAndWebsocket(HttpServer server, Router router) {
    HttpGuiHandler guiHandler = new HttpGuiHandler(server, router);
  }

  private Router createRouter() {
    Router router = Router.router(vertx);

    // to avoid CORS issues of GUI - source: https://github.com/vert-x3/vertx-examples/blob/master/web-examples/src/main/java/io/vertx/example/web/cors/Server.java#L27
    Set<String> allowedHeaders = new HashSet<>();
    allowedHeaders.add("x-requested-with");
    allowedHeaders.add("Access-Control-Allow-Origin");
    allowedHeaders.add("origin");
    allowedHeaders.add("Content-Type");
    allowedHeaders.add("accept");
    allowedHeaders.add("X-PINGARUNER");
    allowedHeaders.add("slug");
    Set<HttpMethod> allowedMethods = new HashSet<>();
    allowedMethods.add(HttpMethod.GET);
    allowedMethods.add(HttpMethod.POST);
    allowedMethods.add(HttpMethod.OPTIONS);
    allowedMethods.add(HttpMethod.DELETE);
    allowedMethods.add(HttpMethod.PATCH);
    allowedMethods.add(HttpMethod.PUT);


    router.route().handler(CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));
    router.route().handler(BodyHandler.create());

    router.get("/").handler((routingContext) -> {
      routingContext.response()
        .setStatusCode(HttpStatus.SC_OK)
        .end("Yggdrasil v0.0");
    });

    HttpEntityHandler handler = new HttpEntityHandler();
    HttpTemplateHandler templateHandler = new HttpTemplateHandler();

    router.get("/environments/:envid").handler(handler::handleGetEntity);
    router.post("/environments/").handler(handler::handleCreateEntity);
    router.put("/environments/:envid").handler(handler::handleUpdateEntity);
    router.delete("/environments/:envid").handler(handler::handleDeleteEntity);

    router.get("/workspaces/:wkspid").handler(handler::handleGetEntity);
    router.post("/workspaces/").handler(handler::handleCreateEntity);
    router.put("/workspaces/:wkspid").handler(handler::handleUpdateEntity);
    router.delete("/workspaces/:wkspid").handler(handler::handleDeleteEntity);

    router.get("/artifacts/templates").handler(templateHandler::handleGetTemplates);
    router.post("/artifacts/templates").handler(templateHandler::handleInstantiateTemp);
    router.get("/artifacts/templates/:classId").handler(templateHandler::handleGetClassDescription);

    router.get("/artifacts/:artid").handler(handler::handleGetEntity);
    router.post("/artifacts/").handler(handler::handleCreateEntity);
    router.put("/artifacts/:artid").handler(handler::handleUpdateEntity);
    // 1st try to delete standard artifact
    router.delete("/artifacts/:artid").handler(handler::handleDeleteEntity);
    // 2nd try to delete instantiated software artifact
    router.delete("/artifacts/:artid").handler(templateHandler::handleDeleteInstance);
    router.put("/artifacts/updateTriples/:artid").handler(templateHandler::handleUpdateTriples);
    // invoke actions on software artifacts defined in the annotations of the corresponding template
    router.put("/artifacts/:artid/*").handler(templateHandler::handleTemplateExtended);

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

      EventBusMessage notification = new EventBusMessage(MessageType.ENTITY_CHANGED_NOTIFICATION)
                                            .setHeader(Headers.REQUEST_IRI, artifactIRI)
                                            .setPayload(routingContext.getBodyAsString());

      vertx.eventBus()
        .publish(EventBusRegistry.NOTIFICATION_DISPATCHER_BUS_ADDRESS, notification.toJson());

      routingContext.response().setStatusCode(HttpStatus.SC_OK).end();
    });
    return router;
  }
}
