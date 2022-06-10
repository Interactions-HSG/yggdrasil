package org.hyperagents.yggdrasil.jason.json;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.StringTerm;
import jason.asSyntax.StringTermImpl;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;
import org.hyperagents.yggdrasil.jason.JsonManager;
import org.hyperagents.yggdrasil.jason.YAgentArch;

public class getStringFromJson extends JsonProcessor {

  @Override
  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {
    Term jsonId = arg[0];
    String attribute = ((StringTerm) arg[1]).getString();
    YAgentArch agArch = (YAgentArch) ts.getAgArch();
    JsonManager jsonManager = agArch.getJsonManager();
    String str = getStringFromJson(jsonId, attribute, jsonManager);
    StringTerm value = new StringTermImpl(str);
    un.bind((VarTerm) arg[2], value);

    return null;
  }

  public String getStringFromJson(Term jsonId, int index, JsonManager jsonManager){
    return getFromJson(jsonId, index,jsonManager ).getAsString();
  }

  public String getStringFromJson(Term jsonId, String attribute, JsonManager jsonManager){
    return getFromJson(jsonId, attribute, jsonManager).getAsString();
  }
}
