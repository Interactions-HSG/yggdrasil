package org.hyperagents.yggdrasil.cartago.artifacts;

import io.vertx.ext.web.RoutingContext;

import java.util.Optional;

public interface HypermediaArtifact {
  String getHypermediaDescription();
  Optional<String> handleAction(String storeResponse, String actionName, RoutingContext context);
}
