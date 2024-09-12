package org.hyperagents.yggdrasil.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Interface to define the API of an Agent in the Yggdrasil environment.
 */
public interface YggdrasilAgent {

  String getName();

  String getAgentUri();

  String getAgentCallbackUri();

  List<String> getFocusedArtifactNames();

  Optional<Path> getMetaData();

}
