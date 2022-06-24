package org.hyperagents.yggdrasil.jason.json;

import com.google.gson.JsonElement;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;
import org.hyperagents.yggdrasil.jason.JsonManager;
import org.hyperagents.yggdrasil.jason.YAgentArch;

public class printJson extends JsonProcessor{

  @Override
  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {

    VarTerm jsonId = (VarTerm) arg[0];
    YAgentArch agArch = (YAgentArch) ts.getAgArch();
    JsonManager jsonManager = agArch.getJsonManager();
    JsonElement jsonElement = getJsonElement(jsonId, jsonManager);
    System.out.println(jsonElement);

    return null;
  }
}
