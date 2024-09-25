package org.hyperagents.yggdrasil.model.impl;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hyperagents.yggdrasil.model.interfaces.AgentBody;

/**
 * Used for config files.
 */
public class AgentBodyImpl implements AgentBody {

  private final Optional<Path> metadata;
  private final List<String> joinedWorkspaces;

  public AgentBodyImpl(Optional<Path> metadata, List<String> joinedWorkspaces) {
    this.joinedWorkspaces = joinedWorkspaces;
    this.metadata = metadata;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AgentBodyImpl agentBody)) {
      return false;
    }

    return Objects.equals(metadata, agentBody.metadata)
      && joinedWorkspaces.equals(agentBody.joinedWorkspaces);
  }

  @Override
  public int hashCode() {
    return Objects.hash(metadata, joinedWorkspaces);
  }

  @Override
  public Optional<Path> getMetadata() {
    return metadata;
  }

  @Override
  public List<String> getJoinedWorkspaces() {
    return joinedWorkspaces;
  }
}
