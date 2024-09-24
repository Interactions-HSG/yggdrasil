package org.hyperagents.yggdrasil.model;

import java.util.List;

/**
 * Interface to define the API of an Agent in the Yggdrasil environment.
 */
public interface YggdrasilAgent {

  String getName();

  String getAgentUri();

  String getAgentCallbackUri();

  List<AgentBody> getBodyConfig();
}
