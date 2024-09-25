package org.hyperagents.yggdrasil.model.impl;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.hyperagents.yggdrasil.model.interfaces.Environment;
import org.hyperagents.yggdrasil.model.interfaces.KnownArtifact;
import org.hyperagents.yggdrasil.model.interfaces.Workspace;
import org.hyperagents.yggdrasil.model.interfaces.YggdrasilAgent;

/**
 * Used for config parsing.
 */
public class EnvironmentImpl  implements Environment {

  private final List<YggdrasilAgent> agents;
  private final List<Workspace> workspaces;
  private final Set<KnownArtifact> knownArtifacts;

  /**
   * Default constructor.
   */
  public EnvironmentImpl(List<YggdrasilAgent> agents, List<Workspace> workspaces,
                         Set<KnownArtifact> knownArtifacts) {
    this.agents = agents;
    this.workspaces = workspaces;
    this.knownArtifacts = knownArtifacts;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EnvironmentImpl that)) {
      return false;
    }
    return Objects.equals(agents, that.agents)
      && Objects.equals(workspaces, that.workspaces)
      && Objects.equals(knownArtifacts, that.knownArtifacts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(agents, workspaces, knownArtifacts);
  }

  @Override
  public List<YggdrasilAgent> getAgents() {
    return this.agents;
  }

  @Override
  public List<Workspace> getWorkspaces() {
    return this.workspaces;
  }

  @Override
  public Set<KnownArtifact> getKnownArtifacts() {
    return this.knownArtifacts;
  }
}
