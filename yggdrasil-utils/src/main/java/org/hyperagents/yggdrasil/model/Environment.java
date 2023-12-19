package org.hyperagents.yggdrasil.model;

import io.vertx.core.shareddata.Shareable;
import java.util.Set;

public interface Environment extends Shareable {
  Set<Workspace> getWorkspaces();

  Set<KnownArtifact> getKnownArtifacts();
}
