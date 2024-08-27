package org.hyperagents.yggdrasil.http;

import io.vertx.ext.web.RoutingContext;

/**
 * Interface to define the API of the HttpEntityHandler.
 */
public interface HttpEntityHandlerInterface {

  void handleRedirectWithoutSlash(final RoutingContext routingContext);

  void handleGetEntity(final RoutingContext routingContext);

  void handleCreateWorkspaceJson(final RoutingContext context);

  void handleCreateWorkspaceTurtle(final RoutingContext context);

  void handleCreateArtifact(final RoutingContext context);

  void handleCreateArtifactJson(final RoutingContext context, final String agentId);

  void handleCreateArtifactTurtle(final RoutingContext context);

  void handleFocus(final RoutingContext context);

  void handleUpdateEntity(final RoutingContext routingContext);

  void handleDeleteEntity(final RoutingContext routingContext);

  void handleEntitySubscription(final RoutingContext routingContext);

  void handleJoinWorkspace(final RoutingContext routingContext);

  void handleLeaveWorkspace(final RoutingContext routingContext);

  void handleCreateSubWorkspace(final RoutingContext context);

  void handleQuery(final RoutingContext routingContext);

  void handleAction(final RoutingContext routingContext);
}
