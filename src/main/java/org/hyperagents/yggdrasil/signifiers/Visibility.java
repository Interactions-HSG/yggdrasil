package org.hyperagents.yggdrasil.signifiers;

import org.hyperagents.signifier.Signifier;

public interface Visibility {
  boolean isVisible(ArtifactState state, AgentProfile profile, Signifier signifier);
}
