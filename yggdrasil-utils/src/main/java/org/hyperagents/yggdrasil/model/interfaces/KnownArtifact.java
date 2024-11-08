package org.hyperagents.yggdrasil.model.interfaces;

/**
 * An interface representing a Joined Agent in the Yggdrasil model.
 *
 * <p>A Joined Agent is a type of agent that has joined a workspace in the Yggdrasil model.
 * Each Joined Agent has a unique name.
 */
public interface KnownArtifact {
  String getClazz();

  String getTemplate();
}
