package org.hyperagents.yggdrasil.model.interfaces;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Interface to define an agentBody.
 */
public interface AgentBody {

  Optional<Path> getMetadata();

  List<String> getJoinedWorkspaces();
}
