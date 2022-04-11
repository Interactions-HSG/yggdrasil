package org.hyperagents.yggdrasil.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
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

    router.route().handler(CorsHandler.create("*")
      .maxAgeSeconds(86400)
      .allowedMethod(io.vertx.core.http.HttpMethod.GET)
      .allowedMethod(io.vertx.core.http.HttpMethod.POST)
      .allowedMethod(io.vertx.core.http.HttpMethod.PUT)
      .allowedMethod(io.vertx.core.http.HttpMethod.DELETE)
      .allowedMethod(io.vertx.core.http.HttpMethod.OPTIONS)
      .allowedHeader("Access-Control-Allow-Headers")
      .allowedHeader("Authorization")
      .allowedHeader("Access-Control-Allow-Method")
      .allowedHeader("Access-Control-Allow-Origin")
      .allowedHeader("Access-Control-Allow-Credentials")
      .allowedHeader("Content-Type")
      .allowedHeader("Expires")
      .allowedHeader("Origin"));

    router.route().handler(BodyHandler.create());


    router.get("/").handler((routingContext) -> routingContext.response()
      .setStatusCode(HttpStatus.SC_OK)
      .end("Yggdrasil v0.0"));

    HttpEntityHandler handler = new HttpEntityHandler(vertx);

    router.get("/workspaces/:wkspid/").handler(handler::handleRedirectWithoutSlash);
    router.get("/workspaces/:wkspid").handler(handler::handleGetEntity);
    router.get("/workspaces").handler(handler::handleGetAllWorkspaces);
    router.post("/workspaces/").consumes("text/turtle")
        .handler(handler::handleCreateEntity);
    router.post("/workspaces/").handler(handler::handleCreateWorkspace);
    router.put("/workspaces/:wkspid").handler(handler::handleUpdateEntity);
    router.delete("/workspaces/:wkspid").handler(handler::handleDeleteEntity);
    //new
    router.put("/workspaces/:wkspid/join").handler(handler::handleJoinWorkspace);
    router.delete("/workspaces/:wkspid/leave").handler(handler::handleLeaveWorkspace);
    router.post("/workspaces/:wkspid/sub").handler(handler::handleCreateSubWorkspace);
    //end new
    router.get("/workspaces/:wkspid/artifacts/:artid/").handler(handler::handleRedirectWithoutSlash);
    router.get("/workspaces/:wkspid/artifacts/:artid").handler(handler::handleGetEntity);
    router.post("/workspaces/:wkspid/artifacts/").consumes("text/turtle")
        .handler(handler::handleCreateEntity);
    router.post("/workspaces/:wkspid/artifacts/").consumes("application/json")
        .handler(handler::handleCreateArtifact);
    router.put("/workspaces/:wkspid/artifacts/:artid").handler(handler::handleUpdateEntity);
    router.delete("/workspaces/:wkspid/artifacts/:artid").handler(handler::handleDeleteEntity);
    router.route("/workspaces/:wkspid/artifacts/:artid/*").handler(handler::handleAction);

    //route agent requests
    router.post("/agents/").consumes("text/plain").handler(handler::handleInstantiateAgent);
    router.post("/agents/:agentid").handler(handler::handleReceiveNotification);
    router.post("/agents/:agentid/message").handler(handler::handleReceiveMessage);
    router.options("/agents/:agentid/message").handler(CorsHandler.create("*")
      .maxAgeSeconds(86400)
      .allowedMethod(io.vertx.core.http.HttpMethod.GET)
      .allowedMethod(io.vertx.core.http.HttpMethod.POST)
      .allowedMethod(io.vertx.core.http.HttpMethod.PUT)
      .allowedMethod(io.vertx.core.http.HttpMethod.DELETE)
      .allowedMethod(io.vertx.core.http.HttpMethod.OPTIONS)
      .allowedHeader("Access-Control-Allow-Headers")
      .allowedHeader("Authorization")
      .allowedHeader("Access-Control-Allow-Method")
      .allowedHeader("Access-Control-Allow-Origin")
      .allowedHeader("Access-Control-Allow-Credentials")
      .allowedHeader("Content-Type")
        .allowedHeader("Expires")
      .allowedHeader("Origin"));

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
