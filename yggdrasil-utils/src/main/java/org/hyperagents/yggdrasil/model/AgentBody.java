package org.hyperagents.yggdrasil.model;

import java.util.List;
import java.util.Optional;

public interface AgentBody {

  Optional<String> getMetadata();
  List<String> getJoinedWorkspaces();
}
