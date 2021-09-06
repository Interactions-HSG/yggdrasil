package org.hyperagents.yggdrasil.signifiers.maze;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.hyperagents.affordance.Affordance;
import org.hyperagents.signifier.Signifier;
import org.hyperagents.util.ReifiedStatement;
import org.hyperagents.util.State;
import org.hyperagents.yggdrasil.signifiers.AgentProfile;
import org.hyperagents.yggdrasil.signifiers.Visibility;

import java.util.List;
import java.util.Optional;

public class VisibilityMaze3 implements Visibility {
  @Override
  public boolean isVisible(Signifier signifier, Model artifactState, AgentProfile profile) {
    boolean b = false;
    System.out.println("print profile");
    System.out.println(profile);
    System.out.println("end profile");
    System.out.println("print signifier");
    System.out.println(signifier.getTextTriples(RDFFormat.TURTLE));
    System.out.println("signifier printed");
    //StructuredSignifier structuredSignifier = StructuredSignifier.getAsStructuredSignifier(signifier);
    List<Affordance> affordances = signifier.getAffordanceList();
    System.out.println("length affordance list: "+affordances.size());
    Affordance affordance = affordances.get(0);
    System.out.println("affordance retrieved");
    ReifiedStatement statement = affordance.getPostcondition().get().getStatementList().get(0);
    System.out.println("statement: "+statement);
    System.out.println("statement defined");
    Value v = statement.getObject();
    int locationSignifier = getRoomNb(v);
    System.out.println("location: "+locationSignifier);
    int goal = 9;
    Model comments = profile.getComments();
    Optional<State> opPurpose = profile.getPurpose();
    State purpose = opPurpose.get();
    goal = getRoomNb(purpose.getStatementList().get(0).getObject());
    /*for (Statement s : comments){
      System.out.println(s);
      Value v2 = s.getObject();
      goal = getRoomNb(v2);

    }*/
    if (locationSignifier == goal){
      b = true;
    }
    return b;
  }

  private int getRoomNb(Value v){
    System.out.println("getRoomNb");
    String s = v.toString();
    System.out.println("value string: "+s);
    String str = s.substring(s.length()-1);
    int n = Integer.parseInt(str);
    System.out.println("room nb: "+n);
    return n;
  }
}
