package org.hyperagents.yggdrasil.signifiers;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.hyperagents.signifier.Signifier;
import org.hyperagents.util.State;

public class VisibilityObjective implements Visibility {
  //The idea of this visibility is to make the signifier visible to the agent if
  // at least one objective of one affordance is equivalent to one purpose of the agent.
  @Override
  public boolean isVisible(Signifier signifier, Model artifactState, AgentProfile profile) {
    boolean b = false;
    State signifierObjective = signifier.getAffordanceList().get(0).getObjective().get();
    State profileObjective = profile.getPurpose().get();
    Resource agent = profile.getAgent();
    if (equivalent(agent, signifierObjective, profileObjective)){
      b = true;
    }
    return b;
  }

//The idea of this method is to consider that the two states are equivalent if agent can be converted in thisAgent.
public boolean equivalent(Resource agent, State state1, State state2){
  boolean b = false;
  if (state1.equals(state2)){
    b = true;
  }
  return b;
}

}
