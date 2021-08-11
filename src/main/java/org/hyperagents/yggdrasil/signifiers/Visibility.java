package org.hyperagents.yggdrasil.signifiers;

import org.eclipse.rdf4j.model.Model;
import org.hyperagents.signifier.Signifier;

public interface Visibility {

  boolean isVisible(Signifier signifier, Model artifactState, AgentProfile profile);
}
