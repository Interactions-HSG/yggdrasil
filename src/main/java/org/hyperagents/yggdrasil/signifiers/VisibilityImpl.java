package org.hyperagents.yggdrasil.signifiers;

import org.eclipse.rdf4j.model.Model;
import org.hyperagents.signifier.Signifier;

public class VisibilityImpl implements  Visibility{
  @Override
  public boolean isVisible(Signifier signifier, Model artifactState, AgentProfile profile) {
    return true;
  }
}
