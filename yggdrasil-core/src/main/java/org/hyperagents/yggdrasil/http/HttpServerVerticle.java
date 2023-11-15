package org.hyperagents.yggdrasil.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import org.apache.http.entity.ContentType;
import org.hyperagents.yggdrasil.utils.impl.HttpInterfaceConfigImpl;

/**
 * This verticle exposes an HTTP/1.1 interface for Yggdrasil. All requests are forwarded to a
 * corresponding handler.
 */
public class HttpServerVerticle extends AbstractVerticle {
  private static final String WORKSPACE_PATH = "/workspaces/:wkspid";
  private static final String ARTIFACT_PATH = "/workspaces/:wkspid/artifacts/:artid";
  private static final String TURTLE_CONTENT_TYPE = "text/turtle";

  private HttpServer server;

  @Override
  public void start(final Promise<Void> startPromise) {
    final var httpConfig = new HttpInterfaceConfigImpl(this.context.config());
    this.server = this.vertx.createHttpServer();
    this.server.requestHandler(this.createRouter())
               .listen(httpConfig.getPort(), httpConfig.getHost())
               .<Void>mapEmpty()
               .onComplete(startPromise);
  }

  @Override
  public void stop(final Promise<Void> stopPromise) {
    this.server.close(stopPromise);
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
                              .allowedHeader("Origin"))
          .handler(BodyHandler.create());

    final var handler = new HttpEntityHandler(this.vertx, this.context);

    router.get("/").handler(handler::handleGetEntity);

    router.post("/workspaces/")
          .consumes(TURTLE_CONTENT_TYPE)
          .handler(handler::handleCreateEntity);
    router.post("/workspaces/").handler(handler::handleCreateWorkspace);

    router.get(WORKSPACE_PATH + "/").handler(handler::handleRedirectWithoutSlash);
    router.get(WORKSPACE_PATH).handler(handler::handleGetEntity);
    router.post(WORKSPACE_PATH + "/").handler(handler::handleRedirectWithoutSlash);
    router.post(WORKSPACE_PATH)
          .consumes(TURTLE_CONTENT_TYPE)
          .handler(handler::handleCreateEntity);
    router.post(WORKSPACE_PATH).handler(handler::handleCreateSubWorkspace);
    router.put(WORKSPACE_PATH + "/").handler(handler::handleRedirectWithoutSlash);
    router.put(WORKSPACE_PATH)
          .consumes(TURTLE_CONTENT_TYPE)
          .handler(handler::handleUpdateEntity);
    router.delete(WORKSPACE_PATH + "/").handler(handler::handleRedirectWithoutSlash);
    router.delete(WORKSPACE_PATH).handler(handler::handleDeleteEntity);

    router.post(WORKSPACE_PATH + "/join/").handler(handler::handleRedirectWithoutSlash);
    router.post(WORKSPACE_PATH + "/join").handler(handler::handleJoinWorkspace);
    router.post(WORKSPACE_PATH + "/leave/").handler(handler::handleRedirectWithoutSlash);
    router.post(WORKSPACE_PATH + "/leave").handler(handler::handleLeaveWorkspace);
    router.post(WORKSPACE_PATH + "/focus/").handler(handler::handleRedirectWithoutSlash);
    router.post(WORKSPACE_PATH + "/focus")
          .consumes(ContentType.APPLICATION_JSON.getMimeType())
          .handler(handler::handleFocus);

    router.post("/workspaces/:wkspid/artifacts/")
          .consumes(TURTLE_CONTENT_TYPE)
          .handler(handler::handleCreateEntity);
    router.post("/workspaces/:wkspid/artifacts/")
          .consumes(ContentType.APPLICATION_JSON.getMimeType())
          .handler(handler::handleCreateArtifact);

    router.get(ARTIFACT_PATH + "/").handler(handler::handleRedirectWithoutSlash);
    router.get(ARTIFACT_PATH).handler(handler::handleGetEntity);
    router.put(ARTIFACT_PATH + "/").handler(handler::handleRedirectWithoutSlash);
    router.put(ARTIFACT_PATH)
          .consumes(TURTLE_CONTENT_TYPE)
          .handler(handler::handleUpdateEntity);
    router.delete(ARTIFACT_PATH + "/").handler(handler::handleRedirectWithoutSlash);
    router.delete(ARTIFACT_PATH).handler(handler::handleDeleteEntity);

    router.post(ARTIFACT_PATH + "/*").handler(handler::handleAction);

    router.post("/hub/").handler(handler::handleEntitySubscription);

    router.get("/query").consumes("text/plain").handler(handler::handleQuery);

    return router;
  }
}
