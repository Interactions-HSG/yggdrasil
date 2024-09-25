package org.hyperagents.yggdrasil.model.impl;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.hyperagents.yggdrasil.model.interfaces.AgentBody;

/**
 * Used for config files.
 */
public class AgentBodyImpl implements AgentBody {

  private final Path metadata;
  private final List<String> joinedWorkspaces;


  /**
   * Default constructor, does path / file validation.
   */
  public AgentBodyImpl(String metadata, List<String> joinedWorkspaces) {
    File f = new File(metadata);
    if (f.exists() && !f.isDirectory()) {
      this.metadata = Path.of(metadata);
    } else {
      System.out.println("unable to identify file for metadata");
      this.metadata = null;
    }
    this.joinedWorkspaces = joinedWorkspaces;
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
  public Path getMetadata() {
    return metadata;
  }

  @Override
  public List<String> getJoinedWorkspaces() {
    return joinedWorkspaces;
  }
}
