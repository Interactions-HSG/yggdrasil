package org.hyperagents.yggdrasil.jason.wot;

import com.google.gson.JsonObject;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;
import org.hyperagents.yggdrasil.jason.JSONLibrary;
import org.hyperagents.yggdrasil.jason.YAgentArch;

public class isValid extends WotAction{

  @Override
  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {
    Term jsonId = arg[0];
    VarTerm bVar = (VarTerm) arg[1];
    boolean b = isValid(jsonId, ts);
    if (b) {
      un.bind(bVar, Literal.LTrue);
    } else {
      un.bind(bVar, Literal.LFalse);
    }
    return null;
  }

  public boolean isValid(com.google.gson.JsonObject responseObject){
    boolean b = false;
    int code = responseObject.get("statusCode").getAsInt();
    if (code >=200 && code<300 ){
      b = true;
    }
    return b;
  }

  public boolean isValid(Term jsonId, TransitionSystem ts){
    YAgentArch agArch = (YAgentArch) ts.getAgArch();
    JsonObject object = agArch.getJsonManager().getJsonElementFromTerm(jsonId).getAsJsonObject();
    return isValid(object);
  }

}
