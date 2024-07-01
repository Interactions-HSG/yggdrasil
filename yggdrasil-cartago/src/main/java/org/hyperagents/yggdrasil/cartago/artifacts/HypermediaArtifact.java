package org.hyperagents.yggdrasil.cartago.artifacts;


import cartago.ArtifactId;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

public interface HypermediaArtifact {
  String getHypermediaDescription();

  ArtifactId getArtifactId();

  Map<String, List<Object>> getArtifactActions();

  Map<String, UnaryOperator<Object>> getResponseConverterMap();

  Optional<String> getMethodNameAndTarget(Object action);

  Optional<String> handleInput(String storeResponse, String actionName, String context);

  Integer handleOutputParams(String storeResponse, String actionName, String context);
}
