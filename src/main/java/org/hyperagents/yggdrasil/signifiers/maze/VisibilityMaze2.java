package org.hyperagents.yggdrasil.signifiers.maze;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.hyperagents.signifier.Signifier;
import org.hyperagents.util.ReifiedStatement;
import org.hyperagents.util.State;
import org.hyperagents.yggdrasil.signifiers.AgentProfile;
import org.hyperagents.yggdrasil.signifiers.Visibility;

import java.util.Optional;

public class VisibilityMaze2 implements Visibility {
  @Override
  public boolean isVisible(Signifier signifier, Model artifactState, AgentProfile profile) {
    boolean b = false;
    Optional<State> precondition = signifier.getAffordanceList().get(0).getPrecondition();
    if (precondition.isPresent()) {
      ReifiedStatement statement = precondition.get().getStatementList().get(0);
      Value v = statement.getObject();
      int locationSignifier = getRoomNb(v);
      System.out.println("location signifier: " + locationSignifier);
      MazeState state = MazeState.createState(artifactState);
      String agentName = getName(profile);
      System.out.println("agent name: " + agentName);
      int location = state.getLocation(agentName);
      System.out.println("location: " + location);
      if (locationSignifier == location) {
        b = true;
      }
      System.out.println("is visible: " + b);
    }
    else {
      System.out.println("precondition is not present");
    }

    return b;
  }

  private int getRoomNb(Value v){
    String s = v.toString();
    String str = s.substring(s.length()-1);
    int n = Integer.parseInt(str);
    return n;
  }

  private String getName(AgentProfile profile){
    String name = profile.getAgent().toString();
    return name;
}
}
