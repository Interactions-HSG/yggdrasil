package org.hyperagents.yggdrasil.jason.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import org.hyperagents.yggdrasil.jason.JSONLibrary;
import org.hyperagents.yggdrasil.jason.JsonManager;
import org.hyperagents.yggdrasil.jason.YAgentArch;

public class makeJson extends JsonProcessor {

  @Override
  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {

    ListTerm attributeList = (ListTerm) arg[0];
    ListTerm valueList = (ListTerm) arg[1];
    VarTerm jsonId = (VarTerm) arg[2];
    createJsonObject(un, ts, attributeList, valueList, jsonId);

    return null;
  }

  public void createJsonObject(Unifier un, TransitionSystem ts,  ListTerm attributeNames, ListTerm attributeValues, VarTerm jsonId){
    YAgentArch agArch = (YAgentArch) ts.getAgArch();
    JsonManager jsonManager = agArch.getJsonManager();
    com.google.gson.JsonObject jsonObject = new com.google.gson.JsonObject();
    int n1 = attributeNames.size();
    int n2 = attributeValues.size();
    if (n1==n2) {
      System.out.println("the sizes are equal");
      for (int i = 0; i < n1; i++) {
        Term attributeName = attributeNames.get(i);
        if (attributeName.isString()) {
          StringTerm attributeNameStringTerm = (StringTerm) attributeName;
          jsonObject.add(attributeNameStringTerm.getString(), getAsJsonElement(attributeValues.get(i), jsonManager));
        }
      }
      String jsonString = jsonObject.toString();
      System.out.println("json created: "+jsonString);
      try {
        jsonManager.new_json(un, jsonString, jsonId);
      } catch (Exception e){
        e.printStackTrace();
      }
    } else {
      System.out.println("the sizes are not equal");
    }
  }



}
