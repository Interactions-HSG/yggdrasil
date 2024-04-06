package org.hyperagents.yggdrasil.cartago.artifacts;

import java.util.Optional;

public interface HypermediaArtifact {
  String getHypermediaDescription();
  Optional<String> handleAction(String storeResponse, String actionName, String context);
}
