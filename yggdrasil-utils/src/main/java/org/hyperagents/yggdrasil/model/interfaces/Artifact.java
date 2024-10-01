package org.hyperagents.yggdrasil.model.interfaces;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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
public interface Artifact {
  String getName();

  Optional<String> getClazz();

  List<?> getInitializationParameters();

  Optional<Path> getRepresentation();

  Optional<Path> getMetaData();

  Optional<YggdrasilAgent> getCreatedBy();

  List<String> getFocusedBy();
}
