package org.hyperagents.yggdrasil.signifiers;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifact;

public class AgentProfileArtifact extends HypermediaArtifact {

  AgentProfile profile;

  public void init(Resource agent){
    profile = new AgentProfile(agent);
  }

  @OPERATION
  public void getAgentProfile(OpFeedbackParam<Object> returnParam){
    returnParam.set(profile);

  }

  @OPERATION
  public void rewrite(Model m){
    this.profile.rewrite(m);
  }

  @Override
  protected void registerInteractionAffordances() {

  }
}
