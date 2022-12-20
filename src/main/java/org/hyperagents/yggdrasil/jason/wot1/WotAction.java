package org.hyperagents.yggdrasil.jason.wot1;

import ch.unisg.ics.interactions.wot.td.clients.TDHttpResponse;
import ch.unisg.ics.interactions.wot.td.schemas.*;
import com.google.gson.*;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.hyperagents.yggdrasil.jason.JSONLibrary;
import org.hyperagents.yggdrasil.jason.JsonManager;
import org.hyperagents.yggdrasil.jason.YAgentArch;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

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

  public com.google.gson.JsonObject sendHttpRequest(String uri, String method, Map<String, String> headers, String body){
    HttpClient client = HttpClients.createDefault();
    AtomicReference<String> returnValue = new AtomicReference();
    com.google.gson.JsonObject returnObject = new com.google.gson.JsonObject();
    ClassicHttpRequest request = new BasicClassicHttpRequest(method, uri);
    for (String key: headers.keySet()){
      String value = headers.get(key);
      request.addHeader(key, value);
    }

    if (body != null){
      System.out.println("body: "+body);
      if (isJson(body)){
        request.addHeader("Content-Type", "application/json");
      }
      request.setEntity(new StringEntity(body));
    }
    try {
      client.execute(request, response -> {
        returnObject.addProperty("statusCode", response.getCode());
        Iterator<Header> responseHeaders = response.headerIterator();
        com.google.gson.JsonObject rHeaders = new com.google.gson.JsonObject();
        while (responseHeaders.hasNext()){
          Header h = responseHeaders.next();
          rHeaders.addProperty(h.getName(), h.getValue());
        }
        returnObject.add("headers", rHeaders);
        System.out.println("response received: ");
        System.out.println(response.toString());
        HttpEntity entity = response.getEntity();
        //String r = EntityUtils.toString(entity);
        BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
        String line = null;
        String s = "";
        while ((line = reader.readLine())!=null){
          s = s + line;
          System.out.println(line);
        }
        System.out.println(response.getEntity().getContent().toString());
        returnValue.set(s);
        return null;
      });
    } catch(Exception e){
      e.printStackTrace();
    }
    System.out.println("request done");
    returnObject.addProperty("body", returnValue.get());
    return returnObject;

  }

  public boolean isJson(String body){
    boolean b = true;
    try {
      JsonParser.parseString(body);
    } catch(JsonParseException e){
      b = false;
      e.printStackTrace();
    }
    return b;
  }
}
