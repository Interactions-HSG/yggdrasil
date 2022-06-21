package org.hyperagents.yggdrasil.jason.wot;

import ch.unisg.ics.interactions.wot.td.clients.TDHttpResponse;
import ch.unisg.ics.interactions.wot.td.schemas.*;
import com.google.gson.*;
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

  public JsonElement getAsJsonElement(String str){
    return JsonParser.parseString(str);
  }

  public DataSchema getDataSchemaFromJsonElement(JsonElement jsonElement){
    DataSchema schema = null;
    if (jsonElement.isJsonObject()){
      JsonObject jsonObject = jsonElement.getAsJsonObject();
      schema = getObjectSchemaFromJsonObject(jsonObject);
    } else if (jsonElement.isJsonArray()){
      JsonArray jsonArray = jsonElement.getAsJsonArray();
      schema = getArraySchemaFromJsonArray(jsonArray);
    } else if (jsonElement.isJsonPrimitive()){
      schema = getDataSchemaFromJsonPrimitive(jsonElement.getAsJsonPrimitive());
    }
    return schema;
  }

  public ObjectSchema getObjectSchemaFromJsonObject(JsonObject jsonObject){
    ObjectSchema.Builder builder = new ObjectSchema.Builder();
    for (String key: jsonObject.keySet()){
      builder.addProperty(key, getDataSchemaFromJsonElement(jsonObject.get(key)));
    }
    return builder.build();
  }

  public ArraySchema getArraySchemaFromJsonArray(JsonArray jsonArray){
    ArraySchema.Builder builder = new ArraySchema.Builder();
    for (int i=0; i<jsonArray.size();i++){
      JsonElement jsonElement = jsonArray.get(i);
      builder.addItem(getDataSchemaFromJsonElement(jsonElement));
    }
    return builder.build();
  }

  public DataSchema getDataSchemaFromJsonPrimitive(JsonPrimitive jsonPrimitive){
    DataSchema schema = null;
    if (jsonPrimitive.isBoolean()){
      schema = new BooleanSchema.Builder().build();
    } else if (jsonPrimitive.isString()){
      schema = new StringSchema.Builder().build();
    } else if (jsonPrimitive.isNumber()){
      schema = new NumberSchema.Builder().build();
    }
    return schema;
  }

  public Map<String, Object> getJsonObjectAsMap(JsonObject jsonObject){
    Map<String, Object> map = new Hashtable<>();
    for (String key: jsonObject.keySet()){
      map.put(key, jsonObject.get(key));
    }
    return map;
  }

  public List<Object> getJsonArrayAsList(JsonArray jsonArray){
    ArrayList<Object> list = new ArrayList<>();
    for (int i=0; i<jsonArray.size();i++){
      list.add(jsonArray.get(i));
    }
    return list;
  }
}
