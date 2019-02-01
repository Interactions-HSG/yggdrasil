package ro.andreiciortea.yggdrasil.http;

import org.apache.http.HttpStatus;

import com.google.common.net.HttpHeaders;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import ro.andreiciortea.yggdrasil.core.EventBusMessage;
import ro.andreiciortea.yggdrasil.core.EventBusMessage.Headers;
import ro.andreiciortea.yggdrasil.core.EventBusMessage.MessageType;
import ro.andreiciortea.yggdrasil.core.EventBusRegistry;

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
    server.requestHandler(router::accept).listen(port, host);
  }

  private Router createRouter() {
    Router router = Router.router(vertx);

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

    // TODO own route for templates
    // TODO create template routes (either dynamically added?? or a generic default handler at the end)
    router.get("/artifacts/templates").handler(templateHandler::handleGetTemplates);
    router.post("/artifacts/templates").handler(templateHandler::handleInstantiateTemp);

    router.get("/artifacts/:artid").handler(handler::handleGetEntity);
    router.post("/artifacts/").handler(handler::handleCreateEntity);
    router.put("/artifacts/:artid").handler(handler::handleUpdateEntity);
    router.delete("/artifacts/:artid").handler(handler::handleDeleteEntity);

    router.put("/artifacts/:artid/*").handler(templateHandler::handleTemplateExtended);


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
