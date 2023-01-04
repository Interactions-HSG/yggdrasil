package org.hyperagents.yggdrasil.jason.json1;

import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Term;
import org.hyperagents.yggdrasil.jason.JsonManager;
import org.hyperagents.yggdrasil.jason.YAgentArch;

public class getStringAsJson extends JsonProcessor {

  @Override
  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {
    StringTerm st = (StringTerm) arg[0];
    Term jsonId = arg[1];
    YAgentArch agArch = (YAgentArch) ts.getAgArch();
    JsonManager jsonManager = agArch.getJsonManager();
    try {
      jsonManager.new_json(un, st.getString(), jsonId);
    } catch(Exception e){
      e.printStackTrace();
    }

    return null;
  }
}
