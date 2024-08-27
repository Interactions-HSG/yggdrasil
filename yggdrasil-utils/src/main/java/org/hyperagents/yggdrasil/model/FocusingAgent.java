package org.hyperagents.yggdrasil.model;

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
public interface FocusingAgent extends JoinedAgent {
  String getCallback();
}
