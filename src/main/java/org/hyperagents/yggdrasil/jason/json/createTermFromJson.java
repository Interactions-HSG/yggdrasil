package org.hyperagents.yggdrasil.jason.json;

import com.google.gson.*;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

public class createTermFromJson extends DefaultInternalAction {

  @Override
  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {
    StringTerm jsonStringTerm = (StringTerm) arg[0];
    String jsonString = jsonStringTerm.getString();
    Term t = createTermFromJson(jsonString);
    VarTerm v = (VarTerm) arg[1];
    un.bind(v, t);

    return true;
  }

  public Term createTermFromJson(String jsonString){
    JsonElement jsonElement = JsonParser.parseString(jsonString);
    return getAsJsonTerm(jsonElement);
  }

  public Term getAsJsonTerm(JsonElement jsonElement){
    Term t = new MapTermImpl();
    if (jsonElement.isJsonPrimitive()){
      JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();
      if (jsonPrimitive.isNumber()){
        t = new NumberTermImpl(jsonPrimitive.getAsDouble());
      } else if (jsonPrimitive.isString()){
        t = new StringTermImpl(jsonPrimitive.getAsString());
      } else if (jsonPrimitive.isBoolean()){
        System.out.println("has boolean");
        boolean b = jsonPrimitive.getAsBoolean();
        if (b){
          t = Literal.LTrue;
          System.out.println("boolean: "+t);
        } else {
          t = Literal.LFalse;
          System.out.println("boolean: "+t);
        }
      }
    } else if (jsonElement.isJsonArray()){
      JsonArray jsonArray = jsonElement.getAsJsonArray();
      ListTerm l =  new ListTermImpl();
      for (int i = 0; i<jsonArray.size(); i++){
        l.add(getAsJsonTerm(jsonArray.get(i)));
      }
      t = l;
    } else if (jsonElement.isJsonObject()){
      com.google.gson.JsonObject jsonObject = jsonElement.getAsJsonObject();
      MapTerm mapTerm = new MapTermImpl();
      for (String key : jsonObject.keySet()){
        mapTerm.put(new StringTermImpl(key), getAsJsonTerm(jsonObject.get(key))); //TODO: check
      }
      t= mapTerm;
    } else if (jsonElement.isJsonNull()){
      JsonNull jsonNull = jsonElement.getAsJsonNull();
    }
    return t;
  }
}
