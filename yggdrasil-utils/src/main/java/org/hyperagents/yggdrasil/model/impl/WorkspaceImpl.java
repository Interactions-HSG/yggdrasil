package org.hyperagents.yggdrasil.model.impl;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.hyperagents.yggdrasil.model.interfaces.Artifact;
import org.hyperagents.yggdrasil.model.interfaces.Workspace;
import org.hyperagents.yggdrasil.model.interfaces.YggdrasilAgent;

/**
 * Used for config parsing.
 */
public class WorkspaceImpl implements Workspace {

  private final String name;
  private final Optional<Path> metaData;
  private final Optional<String> parentName;
  private final Set<Artifact> artifacts;
  private final Set<YggdrasilAgent> agents;
  private final Optional<Path> representation;

  /**
   * Default constructor.
   */
  public WorkspaceImpl(String name, Optional<Path> metaData, Optional<String> parentName,
                       Set<YggdrasilAgent> agents, Set<Artifact> artifacts,
                       Optional<Path> representation) {
    this.name = name;
    this.metaData = metaData;
    this.parentName = parentName;
    this.artifacts = artifacts;
    this.agents = agents;
    this.representation = representation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof WorkspaceImpl workspace)) {
      return false;
    }
    return Objects.equals(name, workspace.name)
      && Objects.equals(metaData, workspace.metaData)
      && Objects.equals(parentName, workspace.parentName)
      && Objects.equals(artifacts, workspace.artifacts)
      && Objects.equals(agents, workspace.agents)
      && Objects.equals(representation, workspace.representation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, metaData, parentName, artifacts, agents, representation);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Optional<Path> getMetaData() {
    return metaData;
  }

  @Override
  public Optional<String> getParentName() {
    return parentName;
  }

  @Override
  public Set<Artifact> getArtifacts() {
    return artifacts;
  }

  @Override
  public Set<YggdrasilAgent> getAgents() {
    return agents;
  }

  @Override
  public Optional<Path> getRepresentation() {
    return representation;
  }
}
