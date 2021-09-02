package org.hyperagents.yggdrasil.signifiers.maze;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.hyperagents.signifier.Signifier;
import org.hyperagents.signifier.StructuredSignifier;
import org.hyperagents.util.RDFS;
import org.hyperagents.util.ReifiedStatement;
import org.hyperagents.util.State;
import org.hyperagents.yggdrasil.signifiers.AgentProfile;
import org.hyperagents.yggdrasil.signifiers.Visibility;

public class VisibilityMaze4 implements Visibility {

  @Override
  public boolean isVisible(Signifier signifier, Model artifactState, AgentProfile profile) {
    System.out.println("check is visible 4");
    boolean b = false;
    if (isVisible1(signifier, artifactState, profile)){
      b = true;
    }
    if (isVisible2(signifier, artifactState, profile)){
      b = true;
    }
    return b;
  }

  private boolean isVisible1(Signifier signifier, Model artifactState, AgentProfile agent){
    boolean b = false;
    int preconditionLocation = getPreconditionLocation(signifier);
    int goalLocation = getGoalLocation(signifier);
    if (preconditionLocation == 1 && goalLocation == getIntermediaryGoal(artifactState, agent)){
      b = true;
    }


    return b;
  }

  private boolean isVisible2(Signifier signifier, Model artifactState, AgentProfile profile){
    boolean b = false;
    int preconditionLocation = getPreconditionLocation(signifier);
    int goalLocation = getGoalLocation(signifier);
    if (preconditionLocation == getIntermediaryGoal(artifactState, profile) && goalLocation == getAgentGoal(artifactState, profile)){
      b = true;
    }
    return b;
  }

  private int getPreconditionLocation(Signifier signifier){
    //StructuredSignifier structuredSignifier = StructuredSignifier.getAsStructuredSignifier(signifier);
    ReifiedStatement statement = signifier.getAffordanceList().get(0).getPrecondition().get().getStatementList().get(0);
    Value v = statement.getObject();
    int preconditionLocation = getRoomNb(v);
    return preconditionLocation;
  }

  private int getGoalLocation(Signifier signifier){
    StructuredSignifier structuredSignifier = StructuredSignifier.getAsStructuredSignifier(signifier);
    ReifiedStatement statement = structuredSignifier.getListAffordances().get(0).getObjective().get().getStatementList().get(0);
    Value v = statement.getObject();
    int objectiveLocation = getRoomNb(v);
    return objectiveLocation;
  }

  private int getRoomNb(Value v){
    String s = v.toString();
    String str = s.substring(s.length()-1);
    int n = Integer.parseInt(str);
    return n;
  }

  private int getAgentGoal(Model artifactState, AgentProfile profile){
    int goal = 9;
    Model comments = profile.getComments();
    State purpose = profile.getPurpose().get();
    for (ReifiedStatement s : purpose.getStatements()){
      if (s.getPredicate().equals(RDFS.rdf.createIRI(EnvironmentOntology.hasFinalGoal))) {
        Value v2 = s.getObject();
        goal = getRoomNb(v2);
      }

    }
    return goal;
  }

  private int getIntermediaryGoal(Model artifactState, AgentProfile profile){
    int goal = 9;
    Model comments = profile.getComments();
    State purpose = profile.getPurpose().get();
    for (ReifiedStatement s : purpose.getStatements()){
      if (s.getPredicate().equals(RDFS.rdf.createIRI(EnvironmentOntology.hasIntermediaryGoal))) {
        Value v2 = s.getObject();
        goal = getRoomNb(v2);
      }

    }
    return goal;
  }

}
