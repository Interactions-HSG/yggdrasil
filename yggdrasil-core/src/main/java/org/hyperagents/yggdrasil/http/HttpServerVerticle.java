package org.hyperagents.yggdrasil.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import org.apache.http.entity.ContentType;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.WebSubConfig;

/**
 * This verticle exposes an HTTP/1.1 interface for Yggdrasil. All requests are forwarded to a
 * corresponding handler.
 */
public class HttpServerVerticle extends AbstractVerticle {

  private static final String WORKSPACE_PATH = "/workspaces/:wkspid";
  private static final String ARTIFACT_PATH = "/workspaces/:wkspid/artifacts/:artid";
  private static final String TURTLE_CONTENT_TYPE = "text/turtle";

  private HttpServer server;
  private EnvironmentConfig environmentConfig;
  private WebSubConfig notificationConfig;

  @Override
  public void start(final Promise<Void> startPromise) {
    final var httpConfig = this.vertx.sharedData()
        .<String, HttpInterfaceConfig>getLocalMap("http-config")
        .get("default");
    this.environmentConfig = this.vertx
      .sharedData()
      .<String, EnvironmentConfig>getLocalMap("environment-config")
      .get("default");
    this.notificationConfig = this.vertx
      .sharedData()
      .<String, WebSubConfig>getLocalMap("notification-config")
      .get("default");
    this.server = this.vertx.createHttpServer();
    this.server.requestHandler(
        this.createRouter(httpConfig, this.environmentConfig, this.notificationConfig)
      )
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
  private Router createRouter(
      final HttpInterfaceConfig httpConfig,
      final EnvironmentConfig environmentConfig,
      final WebSubConfig notificationConfig
  ) {
    final var router = Router.router(this.vertx);
    router.route()
      .handler(SessionHandler.create(LocalSessionStore.create(vertx)))
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

    final HttpEntityHandlerInterface handler = new HttpEntityHandler(
        this.vertx,
        httpConfig,
        environmentConfig,
        notificationConfig
    );


    router.get("/").handler(handler::handleGetEntity);


    router.get("/workspaces/").handler(handler::handleRedirectWithoutSlash);
    router.get("/workspaces").handler(handler::handleGetWorkspaces);

    router.post("/workspaces/")
      .consumes(TURTLE_CONTENT_TYPE)
        .handler(handler::handleCreateWorkspace);

    // workspace paths CRUD
    router.get(WORKSPACE_PATH + "/").handler(handler::handleRedirectWithoutSlash);
    router.get(WORKSPACE_PATH).handler(handler::handleGetEntity);

    router.post(WORKSPACE_PATH + "/").handler(handler::handleRedirectWithoutSlash);
    // TODO: handlecreatesubworkspace also needs to be unified
    router.post(WORKSPACE_PATH)
      .consumes(TURTLE_CONTENT_TYPE)
        .handler(handler::handleCreateSubWorkspace);

    router.put(WORKSPACE_PATH + "/").handler(handler::handleRedirectWithoutSlash);
    router.put(WORKSPACE_PATH)
      .consumes(TURTLE_CONTENT_TYPE)
        .handler(handler::handleUpdateEntity);

    router.delete(WORKSPACE_PATH + "/").handler(handler::handleRedirectWithoutSlash);
    router.delete(WORKSPACE_PATH).handler(handler::handleDeleteEntity);

    router.post(WORKSPACE_PATH + "/join/").handler(handler::handleRedirectWithoutSlash);
    final var joinRoute = router.post(WORKSPACE_PATH + "/join")
        .handler(handler::handleJoinWorkspace);


    router.post(WORKSPACE_PATH + "/leave/").handler(handler::handleRedirectWithoutSlash);
    final var leaveRoute = router.post(WORKSPACE_PATH + "/leave")
        .handler(handler::handleLeaveWorkspace);
    router.post(WORKSPACE_PATH + "/focus/").handler(handler::handleRedirectWithoutSlash);
    final var focusRoute = router.post(WORKSPACE_PATH + "/focus")
        .consumes(ContentType.APPLICATION_JSON.getMimeType())
        .handler(handler::handleFocus);


    router.get("/workspaces/:wkspid/artifacts/").handler(handler::handleRedirectWithoutSlash);
    router.get("/workspaces/:wkspid/artifacts").handler(handler::handleGetArtifacts);

    router.post("/workspaces/:wkspid/artifacts/")
      .consumes(TURTLE_CONTENT_TYPE)
        .handler(handler::handleCreateArtifact);
    final var createArtifactRoute = router.post("/workspaces/:wkspid/artifacts/")
        .handler(handler::handleCreateArtifact);

    // Artifact and Body paths
    router.get(ARTIFACT_PATH + "/").handler(handler::handleRedirectWithoutSlash);
    router.get(ARTIFACT_PATH).handler(handler::handleGetEntity);

    router.put(ARTIFACT_PATH + "/")
        .handler(handler::handleRedirectWithoutSlash);
    router.put(ARTIFACT_PATH)
      .consumes(TURTLE_CONTENT_TYPE)
        .handler(handler::handleUpdateEntity);

    router.delete(ARTIFACT_PATH + "/").handler(handler::handleRedirectWithoutSlash);
    router.delete(ARTIFACT_PATH).handler(handler::handleDeleteEntity);

    final var actionRoute = router.post(ARTIFACT_PATH + "/*").handler(handler::handleAction);


    if (!this.environmentConfig.isEnabled()) {
      joinRoute.disable();
      leaveRoute.disable();
      focusRoute.disable();
      createArtifactRoute.disable();
      actionRoute.disable();
    }

    final var notificationRoute = router.post("/hub/").handler(handler::handleEntitySubscription);

    if (!this.notificationConfig.isEnabled()) {
      notificationRoute.disable();
    }

    router.get("/query").handler(handler::handleQuery);
    router.post("/query")
      .consumes(ContentType.APPLICATION_FORM_URLENCODED.getMimeType())
      .consumes("application/sparql-query")
        .handler(handler::handleQuery);

    return router;
  }

}
