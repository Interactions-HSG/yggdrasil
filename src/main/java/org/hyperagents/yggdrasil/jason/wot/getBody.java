package org.hyperagents.yggdrasil.jason.wot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;
import jason.asSyntax.StringTermImpl;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;
import org.hyperagents.yggdrasil.jason.JSONLibrary;
import org.hyperagents.yggdrasil.jason.YAgentArch;

public class getBody extends WotAction{

  @Override
  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {
    Term jsonId = arg[0];
    String body = getBody(jsonId, ts);
    VarTerm v = (VarTerm ) arg[1];
    un.bind(v, new StringTermImpl(body));
    return null;
  }

  public String getBody(com.google.gson.JsonObject o){
    return o.get("body").getAsString();
  }

  public String getBody(Term jsonId, TransitionSystem ts){
    YAgentArch agArch = (YAgentArch) ts.getAgArch();
    JsonElement e = agArch.getJsonManager().getJsonElementFromTerm(jsonId);
    if (e.isJsonObject()){
      com.google.gson.JsonObject o = e.getAsJsonObject();
      return getBody(o);
    }
    return null;
  }
}
