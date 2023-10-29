package org.hyperagents.yggdrasil.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;

/**
 * This verticle exposes an HTTP/1.1 interface for Yggdrasil. All requests are forwarded to a
 * corresponding handler.
 */
public class HttpServerVerticle extends AbstractVerticle {
  @Override
  public void start() {
    final var httpConfig = new HttpInterfaceConfigImpl(this.context.config());
    this.vertx.createHttpServer().requestHandler(createRouter()).listen(httpConfig.getPort(), httpConfig.getHost());
  }

  /**
   * The HTTP API is defined here when creating the router.
   */
  private Router createRouter() {
    final var router = Router.router(this.vertx);

    router.route().handler(BodyHandler.create());

    router.get("/").handler((routingContext) ->
      routingContext.response().setStatusCode(HttpStatus.SC_OK).end("Yggdrasil v0.0")
    );

    final var handler = new HttpEntityHandler(this.vertx, this.context);

    router.get("/environments/:envid/").handler(handler::handleRedirectWithoutSlash);
    router.get("/environments/:envid").handler(handler::handleGetEntity);

    router.post("/environments/").handler(handler::handleCreateEnvironment);
    router.put("/environments/:envid").handler(handler::handleUpdateEntity);
    router.delete("/environments/:envid").handler(handler::handleDeleteEntity);

    router.get("/environments/:envid/workspaces/:wkspid/").handler(handler::handleRedirectWithoutSlash);
    router.get("/environments/:envid/workspaces/:wkspid").handler(handler::handleGetEntity);
    router
      .post("/environments/:envid/workspaces/")
      .consumes("text/turtle")
      .handler(handler::handleCreateEntity);
    router.post("/environments/:envid/workspaces/").handler(handler::handleCreateWorkspace);
    router.put("/environments/:envid/workspaces/:wkspid").handler(handler::handleUpdateEntity);
    router.delete("/environments/:envid/workspaces/:wkspid").handler(handler::handleDeleteEntity);

    router.get("/environments/:envid/workspaces/:wkspid/artifacts/:artid/").handler(handler::handleRedirectWithoutSlash);
    router.get("/environments/:envid/workspaces/:wkspid/artifacts/:artid").handler(handler::handleGetEntity);
    router
      .post("/environments/:envid/workspaces/:wkspid/artifacts/")
      .consumes("text/turtle")
      .handler(handler::handleCreateEntity);
    router.post("/environments/:envid/workspaces/:wkspid/artifacts/")
      .consumes("application/json")
      .handler(handler::handleCreateArtifact);
    router.put("/environments/:envid/workspaces/:wkspid/artifacts/:artid").handler(handler::handleUpdateEntity);
    router.delete("/environments/:envid/workspaces/:wkspid/artifacts/:artid").handler(handler::handleDeleteEntity);
    router.route("/environments/:envid/workspaces/:wkspid/artifacts/:artid/*").handler(handler::handleAction);

    router.post("/hub/").handler(handler::handleEntitySubscription);

    return router;
  }
}
