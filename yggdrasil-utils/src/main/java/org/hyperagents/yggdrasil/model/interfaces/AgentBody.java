package org.hyperagents.yggdrasil.model.interfaces;

import java.nio.file.Path;
import java.util.List;

/**
 * Interface to define an agentBody.
 */
public interface AgentBody {

  Path getMetadata();

  List<String> getJoinedWorkspaces();
}
