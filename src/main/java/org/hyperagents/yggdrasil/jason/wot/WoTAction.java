package org.hyperagents.yggdrasil.jason.wot;

import ch.unisg.ics.interactions.wot.td.clients.TDHttpRequest;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpResponse;
import com.google.gson.*;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import org.hyperagents.yggdrasil.jason.AgentRegistry;

import java.util.*;

public class WoTAction extends DefaultInternalAction {

  public List<Object> createArrayPayload(JsonArray jsonArray) {
    List<Object> payload = new ArrayList<>();
    for (int i = 0; i < jsonArray.size(); i++) {
      JsonElement e = jsonArray.get(i);
      payload.add(e);
    }
    return payload;
  }

  public Map<String, Object> createObjectPayload(com.google.gson.JsonObject jsonObject) {
    Map<String, Object> payload = new Hashtable<>();
    for (String key : jsonObject.keySet()) {
      JsonElement value = jsonObject.get(key);
      payload.put(key, value);
    }
    return payload;
  }

  public MapTerm createResponseObject(TDHttpResponse response) {
    MapTerm responseObject = new MapTermImpl();
    responseObject.put(new StringTermImpl("statusCode"), new NumberTermImpl(response.getStatusCode()));
    Map<String, String> responseHeaders = response.getHeaders();
    MapTerm rHeaders = new MapTermImpl();
    for (String key : responseHeaders.keySet()) {
      rHeaders.put(new StringTermImpl(key), new StringTermImpl(responseHeaders.get(key)));
    }
    responseObject.put(new StringTermImpl("headers"), rHeaders);
    Optional<String> payload = response.getPayload();
    payload.ifPresent(s -> responseObject.put(new StringTermImpl("body"), new StringTermImpl(s)));
    return responseObject;
  }

  public MapTerm createResponseObject(String url, String method, Map<String, String> headers, String body, TDHttpResponse response) {
    MapTerm requestObject = new MapTermImpl();
    requestObject.put(new StringTermImpl("url"), new StringTermImpl(url));
    requestObject.put(new StringTermImpl("method"), new StringTermImpl(method));
    MapTerm headerObject = new MapTermImpl();
    for (String key: headers.keySet()){
      String value = headers.get(key);
      headerObject.put(new StringTermImpl(key), new StringTermImpl(value));
    }
    requestObject.put(new StringTermImpl("headers"), headerObject);
    if (body != null) {
      requestObject.put(new StringTermImpl("body"), new StringTermImpl(body));
    }
    MapTerm responseObject = new MapTermImpl();
    responseObject.put(new StringTermImpl("statusCode"), new NumberTermImpl(response.getStatusCode()));
    Map<String, String> responseHeaders = response.getHeaders();
    MapTerm rHeaders = new MapTermImpl();
    for (String key : responseHeaders.keySet()) {
      rHeaders.put(new StringTermImpl(key), new StringTermImpl(responseHeaders.get(key)));
    }
    responseObject.put(new StringTermImpl("headers"), rHeaders);
    Optional<String> payload = response.getPayload();
    payload.ifPresent(s -> responseObject.put(new StringTermImpl("body"), new StringTermImpl(s)));
    MapTerm returnObject = new MapTermImpl();
    returnObject.put(new StringTermImpl("request"), requestObject);
    returnObject.put(new StringTermImpl("response"), responseObject);
    return returnObject;
  }

  public String getAsJson(Term t){
    StringBuilder s = new StringBuilder();
    if (t.isMap()){
      MapTerm mt = (MapTerm) t;
      s = new StringBuilder("{");
      for (Term key: mt.keys()){
        String keyString = key.toString();
        String valueString = getAsJson(mt.get(key));
        s.append(keyString).append(":").append(valueString).append(",");
      }
      s = new StringBuilder(s.substring(0, s.length() - 1));
      s.append("}");

    } else if (t.isList()){
      s = new StringBuilder("[");
      ListTerm lt = (ListTerm) t;
      for (Term term: lt){
        s.append(getAsJson(term)).append(",");
      }
      s = new StringBuilder(s.substring(0, s.length() - 1));
      s.append("]");
    } else if (t.isString()){
      s = new StringBuilder(t.toString());
    } else if (t.isNumeric()){
      NumberTerm nt = (NumberTerm) t;
      try {
        double d = nt.solve();
        long r = Math.round(d);
        if (d == (double)r) {
          s = new StringBuilder(String.valueOf(r));
        } else {
          s = new StringBuilder(String.valueOf(d));
        }
      } catch (Exception e){
        System.err.println("The number is not valid");
      }
    } else if (t.isLiteral()){
      s = new StringBuilder(t.toString());
      System.out.println("literal is : "+ s);
    }
    return s.toString();
  }

  public String cleanString(String str){
    String returnString = str;
    if (str.startsWith("\"")){
      returnString = str.substring(1, str.length()-1);
    }
    System.out.println("clean string: "+ returnString);
    return returnString;
  }

  public boolean isJson(String str) { //TODO: check
    boolean b = true;
    try {
      JsonParser.parseString(str);
    } catch (Exception e) {
      b = false;
    }
    return b;
  }

  public Map<String, Object> getAsMap(MapTerm t){
    Map<String, Object> map = new Hashtable<>();
    return map;
  }

  public List<Object> getAsList(ListTerm t){
    List<Object> list = new ArrayList<>();
    return list;
  }

  public JsonElement getAsJsonElement(Term t){
    StringBuilder s = new StringBuilder();
    JsonElement e = null;
    if (t.isMap()){
      JsonObject o = new JsonObject();
      MapTerm mt = (MapTerm) t;
      for (Term key: mt.keys()){
        String keyString = key.toString();
        JsonElement value = getAsJsonElement(mt.get(key));
        o.add(keyString, value);
      }
      e = o;

    } else if (t.isList()){
      JsonArray a = new JsonArray();
      ListTerm lt = (ListTerm) t;
      for (Term term: lt){
        a.add(getAsJsonElement(term));
      }
      e = a;
    } else if (t.isString()){
      JsonPrimitive p = new JsonPrimitive(t.toString());
      e = p;
    } else if (t.isNumeric()){
      NumberTerm nt = (NumberTerm) t;
      try {
        double d = nt.solve();
        long r = Math.round(d);
        if (d == (double)r) {
          JsonPrimitive p = new JsonPrimitive(r);
          e = p;
        } else {
          JsonPrimitive p = new JsonPrimitive(d);
          e = p;
        }
      } catch (Exception ex){
        System.err.println("The number is not valid");
      }
    } else if (t.isLiteral()){
      JsonPrimitive p = new JsonPrimitive(t.toString());
      e = p;
      System.out.println("literal is : "+ s);
    }
    return e;
  }


}
