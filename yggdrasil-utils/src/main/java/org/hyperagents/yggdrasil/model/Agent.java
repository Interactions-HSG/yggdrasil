package org.hyperagents.yggdrasil.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface Agent {

  String getName();
  String getAgentUri();
  String getAgentCallbackUri();
  List<String> getFocusedArtifactNames();
  Optional<Path> getMetaData();

}
