package org.hyperagents.yggdrasil.signifiers.maze;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.hyperagents.signifier.Signifier;
import org.hyperagents.signifier.StructuredSignifier;
import org.hyperagents.util.ReifiedStatement;
import org.hyperagents.yggdrasil.signifiers.AgentProfile;
import org.hyperagents.yggdrasil.signifiers.Visibility;

public class VisibilityMaze3 implements Visibility {
  @Override
  public boolean isVisible(Signifier signifier, Model artifactState, AgentProfile profile) {
    boolean b = false;
    StructuredSignifier structuredSignifier = StructuredSignifier.getAsStructuredSignifier(signifier);
    ReifiedStatement statement = structuredSignifier.getListAffordances().get(0).getObjective().get().getStatementList().get(0);
    Value v = statement.getObject();
    int locationSignifier = getRoomNb(v);
    int goal = 9;
    Model comments = profile.getComments();
    for (Statement s : comments){
      Value v2 = s.getObject();
      goal = getRoomNb(v2);

    }
    if (locationSignifier == goal){
      b = true;
    }
    return b;
  }

  private int getRoomNb(Value v){
    String s = v.toString();
    String str = s.substring(s.length()-1);
    int n = Integer.parseInt(str);
    return n;
  }
}
