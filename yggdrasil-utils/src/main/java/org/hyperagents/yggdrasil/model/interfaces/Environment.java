package org.hyperagents.yggdrasil.model.interfaces;

import io.vertx.core.shareddata.Shareable;
import java.util.List;
import java.util.Set;

/**
 * An interface representing an Artifact in the Yggdrasil model.
 *
 * <p>An Artifact is a component of the Yggdrasil model that has a name, a class, initialization
 * parameters,
 * a set of focusing agents, and a representation.
 *
 * <p>The class, initialization parameters, and representation are optional and may not be
 * present for all artifacts.
 */
public interface Environment extends Shareable {
  List<YggdrasilAgent> getAgents();

  List<Workspace> getWorkspaces();

  Set<KnownArtifact> getKnownArtifacts();
}
