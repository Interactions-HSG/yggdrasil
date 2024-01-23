package org.hyperagents.yggdrasil.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface Artifact {
  String getName();

  Optional<String> getClazz();

  List<?> getInitializationParameters();

  Set<FocusingAgent> getFocusingAgents();

  Optional<Path> getRepresentation();
}
