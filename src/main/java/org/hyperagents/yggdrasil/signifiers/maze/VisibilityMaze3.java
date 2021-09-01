package org.hyperagents.yggdrasil.signifiers.maze;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.hyperagents.affordance.Affordance;
import org.hyperagents.signifier.Signifier;
import org.hyperagents.util.ReifiedStatement;
import org.hyperagents.yggdrasil.signifiers.AgentProfile;
import org.hyperagents.yggdrasil.signifiers.Visibility;

import java.util.List;

public class VisibilityMaze3 implements Visibility {
  @Override
  public boolean isVisible(Signifier signifier, Model artifactState, AgentProfile profile) {
    boolean b = false;
    System.out.println("print profile");
    System.out.println(profile);
    System.out.println("end profile");
    //StructuredSignifier structuredSignifier = StructuredSignifier.getAsStructuredSignifier(signifier);
    List<Affordance> affordances = signifier.getAffordanceList();
    System.out.println("length affordance list: "+affordances.size());
    Affordance affordance = affordances.get(0);
    System.out.println("affordance retrieved");
    ReifiedStatement statement = affordance.getObjective().get().getStatementList().get(0);
    System.out.println("statement defined");
    Value v = statement.getObject();
    int locationSignifier = getRoomNb(v);
    System.out.println("location: "+locationSignifier);
    int goal = 9;
    Model comments = profile.getComments();
    for (Statement s : comments){
      System.out.println(s);
      Value v2 = s.getObject();
      goal = getRoomNb(v2);

    }
    if (locationSignifier == goal){
      b = true;
    }
    return b;
  }

  private int getRoomNb(Value v){
    System.out.println("getRoomNb");
    String s = v.toString();
    String str = s.substring(s.length()-1);
    int n = Integer.parseInt(str);
    System.out.println("room nb: "+n);
    return n;
  }
}
