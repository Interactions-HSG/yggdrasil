package org.hyperagents.yggdrasil.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;

/**
 * This verticle exposes an HTTP/1.1 interface for Yggdrasil. All requests are forwarded to a
 * corresponding handler.
 */
public class HttpServerVerticle extends AbstractVerticle {
  private static final String ARTIFACT_PATH = "/workspaces/:wkspid/artifacts/:artid";

  @Override
  public void start() {
    final var httpConfig = new HttpInterfaceConfigImpl(this.context.config());
    this.vertx.createHttpServer()
              .requestHandler(createRouter())
              .listen(httpConfig.getPort(), httpConfig.getHost());
  }

  /**
   * The HTTP API is defined here when creating the router.
   */
  private Router createRouter() {
    final var router = Router.router(this.vertx);
    router.route()
          .handler(CorsHandler.create()
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
    router.get("/").handler(ctx -> ctx.response()
                                      .setStatusCode(HttpStatus.SC_OK)
                                      .end("Yggdrasil v0.0.0"));
    final var handler = new HttpEntityHandler(this.vertx, this.context);
    router.get("/workspaces/:wkspid/").handler(handler::handleRedirectWithoutSlash);
    router.get("/workspaces/:wkspid").handler(handler::handleGetEntity);
    router.post("/workspaces/")
          .consumes("text/turtle")
          .handler(handler::handleCreateEntity);
    router.post("/workspaces/").handler(handler::handleCreateWorkspace);
    router.put("/workspaces/:wkspid").handler(handler::handleUpdateEntity);
    router.delete("/workspaces/:wkspid").handler(handler::handleDeleteEntity);
    router.put("/workspaces/:wkspid/join").handler(handler::handleJoinWorkspace);
    router.delete("/workspaces/:wkspid/leave").handler(handler::handleLeaveWorkspace);
    router.post("/workspaces/:wkspid/sub").handler(handler::handleCreateSubWorkspace);
    router.post("/workspaces/:wkspid/focus").handler(handler::handleFocus);
    router.get("/workspaces/:wkspid/artifacts/:artid/")
          .handler(handler::handleRedirectWithoutSlash);
    router.get(ARTIFACT_PATH).handler(handler::handleGetEntity);
    router.put(ARTIFACT_PATH).handler(handler::handleUpdateEntity);
    router.post("/workspaces/:wkspid/artifacts/")
          .consumes("text/turtle")
          .handler(handler::handleCreateEntity);
    router.post("/workspaces/:wkspid/artifacts/")
          .consumes("application/ld+json")
          .handler(handler::handleCreateEntity);
    router.post("/workspaces/:wkspid/artifacts/")
          .consumes("application/json")
          .handler(handler::handleCreateArtifact);
    router.put(ARTIFACT_PATH).handler(handler::handleUpdateEntity);
    router.delete(ARTIFACT_PATH).handler(handler::handleDeleteEntity);
    router.route("/workspaces/:wkspid/artifacts/:artid/*").handler(handler::handleAction);
    router.post("/hub/").handler(handler::handleEntitySubscription);
    return router;
  }
}
