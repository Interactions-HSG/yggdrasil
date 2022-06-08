package org.hyperagents.yggdrasil.jason.wot;

import ch.unisg.ics.interactions.wot.td.clients.TDHttpResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;
import org.hyperagents.yggdrasil.jason.JSONLibrary;
import org.hyperagents.yggdrasil.jason.JsonManager;
import org.hyperagents.yggdrasil.jason.YAgentArch;

import java.util.*;

public class WotAction extends DefaultInternalAction {

  public void bindTermToJson(Term jsonId, JsonElement jsonElement, Unifier un, TransitionSystem ts){
    YAgentArch agArch = (YAgentArch) ts.getAgArch();
    JsonManager jsonManager = agArch.getJsonManager();
    try {
      jsonManager.new_json(un, jsonElement.toString(), jsonId);
    } catch(Exception e){
      e.printStackTrace();
    }
  }

  List<Object> createArrayPayload(JsonArray jsonArray){
    List<Object> payload = new ArrayList<>();
    for (int i = 0; i<jsonArray.size();i++){
      JsonElement e = jsonArray.get(i);
      payload.add(e);
    }
    return payload;
  }

  Map<String, Object> createObjectPayload(com.google.gson.JsonObject jsonObject){
    Map<String, Object> payload = new Hashtable<>();
    for (String key: jsonObject.keySet()){
      JsonElement value = jsonObject.get(key);
      payload.put(key, value);
    }
    return payload;
  }

  public com.google.gson.JsonObject createResponseObject(TDHttpResponse response){
    com.google.gson.JsonObject responseObject = new com.google.gson.JsonObject();
    responseObject.addProperty("statusCode", response.getStatusCode());
    Map<String,String> responseHeaders = response.getHeaders();
    com.google.gson.JsonObject rHeaders = new com.google.gson.JsonObject();
    for (String key: responseHeaders.keySet()){
      rHeaders.addProperty(key, responseHeaders.get(key));
    }
    responseObject.add("headers", rHeaders);
    Optional<String> payload = response.getPayload();
    if (payload.isPresent()){
      responseObject.addProperty("body", payload.get());
    }
    return responseObject;
  }
}
