package org.hyperagents.yggdrasil.model;

import java.util.List;

public interface Agent {

  String getName();
  String getAgentUri();
  String getAgentCallbackUri();
  List<String> getFocusedArtifactNames();

}
