package org.hyperagents.yggdrasil.jason;

import jason.architecture.AgArch;
import jason.asSemantics.ActionExec;
import jason.asSemantics.Intention;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;

import java.util.Iterator;
import java.util.List;

public class YAgentArch extends AgArch {

  @Override
  public void act(ActionExec actionExec){
    String agentName = getTS().getUserAgArch().getAgName();
    Intention currentIntention = getTS().getC().getSelectedIntention();

    Structure action = actionExec.getActionTerm();

    try {
      boolean failed = false;
      ListTerm lt = action.getAnnots();
      if (lt != null){
        Iterator<Term> it = lt.iterator();
        while (it.hasNext()){
          Term annot = it.next();
        }
      }
      String func = action.getFunctor();
      List<Term> terms = action.getTerms();
      if (func.equals("makeArtifactArtifact")){
        //cartagoHandler.createArtifact(agentName, terms.get(0).toString(), terms.get(1).toString(), terms.get(2).toString(), Promise.promise());
      }

    } catch(Exception e){
      e.printStackTrace();
    }
  }

  boolean isEnvironmentOperation(String o){
    return o.equals("joinWorkspace") ||
      o.equals("createWorkspace")||
      o.equals("createSubWorkspace")||
      //o.equals("lookupArtifact") ||
      o.equals("makeArtifact")||
      o.equals("focus")||
      //o.equals("focusWhenAvailable")||
      o.equals("leaveWorkspace");
  }

  //Syntax for the operations
  //joinWorkspace(String workspaceName)
  //createWorkspace(String workspaceName)
  //createSubWorkspace(String workspaceName, String subWorkspaceName)
  //makeArtifact(String workspaceName, String artifactName)
  //focus(String workspaceName, String artifactname)
  //leaveWorkspace(String workspaceName)
}
