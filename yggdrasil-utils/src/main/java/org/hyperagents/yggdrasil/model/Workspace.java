package org.hyperagents.yggdrasil.model;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

/**
 * An interface representing a Joined Agent in the Yggdrasil model.
 *
 * <p>A Joined Agent is a type of agent that has joined a workspace in the Yggdrasil model.
 * Each Joined Agent has a unique name.
 */
public interface Workspace {
  String getName();

  Optional<String> getParentName();

  Set<Artifact> getArtifacts();

  Set<JoinedAgent> getAgents();

  Optional<Path> getRepresentation();
}
