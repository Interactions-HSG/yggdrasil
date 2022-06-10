package org.hyperagents.yggdrasil.jason.json;

import com.google.gson.JsonElement;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.StringTerm;
import jason.asSyntax.StringTermImpl;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;
import org.hyperagents.yggdrasil.jason.JSONLibrary;
import org.hyperagents.yggdrasil.jason.JsonManager;
import org.hyperagents.yggdrasil.jason.YAgentArch;

public class getJsonAsString extends JsonProcessor {

  @Override
  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {
    Term jsonId = arg[0];
    YAgentArch agArch = (YAgentArch) ts.getAgArch();
    JsonManager jsonManager = agArch.getJsonManager();
    StringTerm str = getAsStringTerm(jsonId, jsonManager);
    un.bind((VarTerm) arg[1], str);

    return null;
  }

  public StringTerm getAsStringTerm(JsonElement jsonElement){
    System.out.println(jsonElement);
    String jsonElementString = jsonElement.toString();
    StringTerm st =  new StringTermImpl(jsonElementString);
    System.out.println("string term: "+st);
    return st;
  }

  public StringTerm getAsStringTerm(Term jsonId, JsonManager jsonManager){
    JsonElement json = jsonManager.getJsonElementFromTerm(jsonId);
    return getAsStringTerm(json);
  }
}
