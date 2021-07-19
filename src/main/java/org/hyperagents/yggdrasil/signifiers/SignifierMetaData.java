package org.hyperagents.yggdrasil.signifiers;

import cartago.AgentId;

public class SignifierMetaData {

  AgentId agent;

  public SignifierMetaData(AgentId agent) {
    this.agent=agent;
  }

  public boolean hasAgent(AgentId ag){
    boolean b=false;
    if (this.agent.equals(ag)){
      b=true;
    }
    return b;
  }
}
