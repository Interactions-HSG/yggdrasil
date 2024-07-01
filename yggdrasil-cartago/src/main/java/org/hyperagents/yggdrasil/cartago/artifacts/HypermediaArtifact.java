package org.hyperagents.yggdrasil.cartago.artifacts;


import java.util.Optional;

public interface HypermediaArtifact {
  String getHypermediaDescription();

  Optional<String> handleInput(String storeResponse, String actionName, String context);

  Integer handleOutputParams(String storeResponse, String actionName, String context);
}
