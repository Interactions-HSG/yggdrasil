package org.hyperagents.yggdrasil.jason.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import jason.asSemantics.DefaultInternalAction;
import jason.asSyntax.*;
import org.hyperagents.yggdrasil.jason.JsonManager;

public class JsonProcessor extends DefaultInternalAction {

  public JsonElement getJsonElement(Term jsonId, JsonManager jsonManager){
    return jsonManager.getJsonElementFromTerm(jsonId);


  }

  public JsonElement getAsJsonElement(Term t, JsonManager jsonManager){
    JsonElement element = null;
    if (t.isString()){
      StringTerm st = (StringTerm) t;
      element = new JsonPrimitive(st.getString());
    } else if (t.isNumeric()){
      NumberTerm nt = (NumberTerm) t;
      try {
        double num = nt.solve();
        element = new JsonPrimitive(num);
        if (num == (int) num){
          System.out.println("is integer");
          int n = (int) num;
          element = new JsonPrimitive(n);
        }
      }
      catch(Exception e){
        e.printStackTrace();
      }
    } else if (t.isList()){
      ListTerm listTerm = (ListTerm) t;
      JsonArray array = new JsonArray();
      for (int i = 0; i<listTerm.size(); i++){
        array.add(getAsJsonElement(listTerm.get(i), jsonManager));
      }
      element = array;
    } else if (t.isVar()){
      element = getJsonElement(t, jsonManager);
    } else if (t.isLiteral()){
      Literal l = (Literal) t;
      String func = l.getFunctor();
      if (func.equals("true")){
        element = new JsonPrimitive(true);
      } else if (func.equals("false")){
        element = new JsonPrimitive(false);
      } else {
        element = getJsonElement(t, jsonManager);
      }
    }
    return element;
  }

  public JsonElement getFromJson(Term jsonId, String attribute, JsonManager jsonManager){
    JsonElement jsonElement = getJsonElement(jsonId, jsonManager);
    if (jsonElement.isJsonObject()){
      com.google.gson.JsonObject jsonObject = jsonElement.getAsJsonObject();
      return jsonObject.get(attribute);
    }
    return null;
  }

  public JsonElement getFromJson(Term jsonId, int index, JsonManager jsonManager){
    JsonElement jsonElement = getJsonElement(jsonId, jsonManager);
    if (jsonElement.isJsonArray()){
      JsonArray jsonArray = jsonElement.getAsJsonArray();
      return jsonArray.get(index);
    }
    return null;
  }
}
