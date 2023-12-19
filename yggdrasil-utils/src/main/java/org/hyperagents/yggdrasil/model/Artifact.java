package org.hyperagents.yggdrasil.model;

import java.util.List;
import java.util.Set;

public interface Artifact {
  String getName();

  String getClazz();

  List<?> getInitializationParameters();

  Set<FocusingAgent> getFocusingAgents();
}
