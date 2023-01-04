package org.hyperagents.yggdrasil.jason.json1;

import com.google.gson.JsonElement;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;
import org.hyperagents.yggdrasil.jason.JsonManager;
import org.hyperagents.yggdrasil.jason.YAgentArch;

public class hasAttribute extends JsonProcessor {

  @Override
  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {
    Term jsonId = arg[0];
    StringTerm attributeTerm = (StringTerm) arg[1];
    String attribute = attributeTerm.getString();
    YAgentArch agArch = (YAgentArch) ts.getAgArch();
    JsonManager jsonManager = agArch.getJsonManager();
    boolean b = hasAttribute(jsonId, attribute, jsonManager);
    VarTerm v = (VarTerm) arg[2];
    Term t = Literal.LFalse;
    if (b){
      t = Literal.LTrue;
    }
    un.bind(v,t);

    return null;
  }

  public boolean hasAttribute(Term jsonId, String attribute, JsonManager jsonManager){
    boolean b = false;
    JsonElement jsonElement = getJsonElement(jsonId, jsonManager);
    if (jsonElement.isJsonObject()){
      b = jsonElement.getAsJsonObject().keySet().contains(attribute);
    }
    return b;
  }
}
