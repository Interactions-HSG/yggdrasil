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
  public EnvironmentImpl(final List<YggdrasilAgent> agents, final List<Workspace> workspaces,
                         final Set<KnownArtifact> knownArtifacts) {
    this.agents = List.copyOf(agents);
    this.workspaces = List.copyOf(workspaces);
    this.knownArtifacts = Set.copyOf(knownArtifacts);
  }

  @Override
  public boolean equals(final Object o) {
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
    return List.copyOf(agents);
  }

  @Override
  public List<Workspace> getWorkspaces() {
    return List.copyOf(workspaces);
  }

  @Override
  public Set<KnownArtifact> getKnownArtifacts() {
    return Set.copyOf(knownArtifacts);
  }
}
