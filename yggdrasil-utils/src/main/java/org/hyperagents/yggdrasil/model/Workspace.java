package org.hyperagents.yggdrasil.model;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

/**
 * TODO: Javadoc.
 */
public interface Workspace {
  String getName();

  Optional<String> getParentName();

  Set<Artifact> getArtifacts();

  Set<JoinedAgent> getAgents();

  Optional<Path> getRepresentation();
}
