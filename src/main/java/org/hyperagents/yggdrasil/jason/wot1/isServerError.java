package org.hyperagents.yggdrasil.jason.wot1;

import com.google.gson.JsonObject;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;
import org.hyperagents.yggdrasil.jason.YAgentArch;

public class isServerError extends  WotAction{

  @Override
  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {
    Term jsonId = arg[0];
    VarTerm bVar = (VarTerm) arg[1];
    boolean b = isServerError(jsonId, ts);
    if (b) {
      un.bind(bVar, Literal.LTrue);
    } else {
      un.bind(bVar, Literal.LFalse);
    }
    return null;
  }

  public boolean isServerError(Term jsonId, TransitionSystem ts){
    YAgentArch agArch = (YAgentArch) ts.getAgArch();
    JsonObject object = agArch.getJsonManager().getJsonElementFromTerm(jsonId).getAsJsonObject();
    return isServerError(object);
  }

  public boolean isServerError(com.google.gson.JsonObject responseObject){
    boolean b = false;
    int code = responseObject.get("statusCode").getAsInt();
    if (code >500 ){
      b = true;
    }
    return b;
  }
}
