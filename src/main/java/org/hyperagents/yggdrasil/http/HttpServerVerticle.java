package org.hyperagents.yggdrasil.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.http.HttpStatus;

/**
 * This verticle exposes an HTTP/1.1 interface for Yggdrasil. All requests are forwarded to a
 * corresponding handler.
 */
public class HttpServerVerticle extends AbstractVerticle {

  @Override
  public void start() {
    HttpServer server = vertx.createHttpServer();

    Router router = createRouter();
    HttpInterfaceConfig httpConfig = new HttpInterfaceConfig(config());
    server.requestHandler(router).listen(httpConfig.getPort(), httpConfig.getHost());
  }

  /**
   * The HTTP API is defined here when creating the router.
   */
  private Router createRouter() {
    Router router = Router.router(vertx);

    router.route().handler(BodyHandler.create());

    router.get("/").handler((routingContext) -> routingContext.response()
      .setStatusCode(HttpStatus.SC_OK)
      .end("Yggdrasil v0.0"));

    HttpEntityHandler handler = new HttpEntityHandler(vertx);

    router.get("/environments/:envid").handler(handler::handleGetEntity);
    router.post("/environments/").handler(handler::handleCreateEntity);
//    router.post("/environments/").handler(handler::handleCreateEnvironment);
    router.put("/environments/:envid").handler(handler::handleUpdateEntity);
    router.delete("/environments/:envid").handler(handler::handleDeleteEntity);

    router.get("/environments/:envid/workspaces/:wkspid").handler(handler::handleGetEntity);
    router.post("/environments/:envid/workspaces/").consumes("text/turtle")
        .handler(handler::handleCreateEntity);
    router.post("/environments/:envid/workspaces/").handler(handler::handleCreateWorkspace);
    router.put("/environments/:envid/workspaces/:wkspid").handler(handler::handleUpdateEntity);
    router.delete("/environments/:envid/workspaces/:wkspid").handler(handler::handleDeleteEntity);

    router.get("/environments/:envid/workspaces/:wkspid/artifacts/:artid").handler(handler::handleGetEntity);
    router.post("/environments/:envid/workspaces/:wkspid/artifacts/").consumes("text/turtle")
        .handler(handler::handleCreateEntity);
    router.post("/environments/:envid/workspaces/:wkspid/artifacts/").consumes("application/json")
        .handler(handler::handleCreateArtifact);
    router.put("/environments/:envid/workspaces/:wkspid/artifacts/:artid").handler(handler::handleUpdateEntity);
    router.delete("/environments/:envid/workspaces/:wkspid/artifacts/:artid").handler(handler::handleDeleteEntity);
    router.route("/environments/:envid/workspaces/:wkspid/artifacts/:artid/*").handler(handler::handleAction);

    // route artifact manual requests
    // TODO: this feature was implemented for the WWW2020 demo, a manual is any RDF graph
    router.get("/manuals/:wkspid").handler(handler::handleGetEntity);
    router.post("/manuals/").handler(handler::handleCreateEntity);
    router.put("/manuals/:wkspid").handler(handler::handleUpdateEntity);
    router.delete("/manuals/:wkspid").handler(handler::handleDeleteEntity);

    router.post("/hub/").handler(handler::handleEntitySubscription);

    return router;
  }
}
