package org.hyperagents.yggdrasil.model;

import io.vertx.core.shareddata.Shareable;
import java.util.List;
import java.util.Set;

/**
 * TODO: Javadoc.
 */
public interface Environment extends Shareable {
  List<Workspace> getWorkspaces();

  Set<KnownArtifact> getKnownArtifacts();
}
