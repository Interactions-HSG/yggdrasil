package org.hyperagents.yggdrasil.jason.json1;

import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import org.hyperagents.yggdrasil.jason.JsonManager;
import org.hyperagents.yggdrasil.jason.YAgentArch;

public class getNumberFromJson extends JsonProcessor {

  @Override
  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {
    Term jsonId = arg[0];
    String attribute = ((StringTerm) arg[1]).getString();
    YAgentArch agArch = (YAgentArch) ts.getAgArch();
    JsonManager jsonManager = agArch.getJsonManager();
    NumberTerm value = new NumberTermImpl(getNumberFromJson(jsonId, attribute, jsonManager));
    un.bind((VarTerm) arg[2], value);

    return null;
  }

  public double getNumberFromJson(Term jsonId, int index, JsonManager jsonManager){
    return getFromJson(jsonId, index, jsonManager).getAsDouble();
  }

  public double getNumberFromJson(Term jsonId, String attribute, JsonManager jsonManager){
    return getFromJson(jsonId, attribute, jsonManager).getAsDouble();
  }
}
