package org.hyperagents.yggdrasil.model.interfaces;

import java.util.List;
import java.util.Optional;

/**
 * Interface to define the API of an Agent in the Yggdrasil environment.
 */
public interface YggdrasilAgent {

  String getName();

  String getAgentUri();

  Optional<String> getAgentCallbackUri();

  List<AgentBody> getBodyConfig();
}
