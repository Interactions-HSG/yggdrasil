package org.hyperagents.yggdrasil.jason;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class JSONLibrary implements Serializable {

  private HashMap<String, JsonElement> currentJsons;

  private HashMap<JsonElement, Atom> currentJsons_inverse;

  private long id;

  public JSONLibrary(){
    this.currentJsons = new HashMap<>();
    this.currentJsons_inverse = new HashMap<>();
    this.id = 0;
  }

  public Term getNewJsonId(){
    Term jsonId = new NumberTermImpl(id);
    id++;
    return jsonId;
  }

  public boolean new_json(Unifier un, String jsonString, Term id) throws Exception {
    try {
      JsonElement jsonElement = JsonParser.parseString(jsonString);
      return bindJson(un, id, jsonElement);
    } catch(Exception e){
      e.printStackTrace();
      throw new Exception();
    }
  }

  public JsonElement get_json(Unifier un, Term objId) throws Exception {
    JsonElement jsonElement = null;
    if (objId.isAtom()){
      String objName = ((Atom)objId).getFunctor();
      synchronized (currentJsons){
        jsonElement = currentJsons.get(objName);
      }
      if (jsonElement != null) {
        return jsonElement;
      } else {
        throw new Exception();
      }
    }  else {
      throw new Exception();
    }
  }

  public boolean bindJson(Unifier un, Term id, JsonElement jsonElement) {
    // null object are considered to _ variable
    if (jsonElement == null) {
      return un.unifies(id, new jason.asSyntax.VarTerm("_"));
    }
    // already registered object?
    synchronized (currentJsons){
      Term aKey = currentJsons_inverse.get(jsonElement);
      if (aKey != null) {
        // object already referenced -> unifying terms
        // referencing the object
        //log("obj already registered: unify "+id+" "+aKey);
        return un.unifies(id, (Term) aKey);
      } else {
        // object not previously referenced
        if (id.isVar()) {
          // get a ground term
          Atom idTerm = generateFreshId();
          un.unifies(id, idTerm);
          registerDynamic(idTerm, jsonElement);
          //log("not ground id for a new obj: "+id+" as ref for "+obj);
          return true;
        } else {
          // verify of the id is already used
          Atom id2 = (Atom)id;
          JsonElement linkedJson = currentJsons.get(id2.getFunctor());
          if (linkedJson == null) {
            registerDynamic(id2, linkedJson);
            //log("ground id for a new obj: "+id+" as ref for "+obj);
            return true;
          } else {
            // an object with the same id is already
            // present: must be the same object
            return jsonElement == linkedJson;
          }
        }
      }
    }
  }

  /**
   * Generates a fresh numeric identifier
   * @return
   */
  protected Atom generateFreshId() {
    return new Atom("cobj_" + id++);
  }

  public void registerDynamic(Atom id, JsonElement jsonElement) {
    synchronized (currentJsons){
      currentJsons.put(id.getFunctor(), jsonElement);
      currentJsons_inverse.put(jsonElement, id);
    }
  }

  public boolean isJson(String str){
    boolean b = true;
    try {
      JsonParser.parseString(str);
    } catch (Exception e){
      b = false;
    }
    return b;
  }

  public JsonElement getJSONFromString(String jsonString) throws Exception {
    try {
      JsonElement jsonElement = JsonParser.parseString(jsonString);
      return jsonElement;
    } catch(Exception e){
      throw new Exception();
    }
  }

  public JsonElement get(Unifier un, Term jsonId, int i) throws Exception {
    try {
      JsonElement jsonElement = get_json(un, jsonId);
      JsonArray array = jsonElement.getAsJsonArray();
      return array.get(i);
    } catch (Exception e){
      throw new Exception();
    }

  }

  public JsonElement get(Unifier un, Term jsonId, String attribute) throws Exception {
    try {
      JsonElement jsonElement = get_json(un, jsonId);
      JsonObject object = jsonElement.getAsJsonObject();
      return object.get(attribute);
    } catch (Exception e){
      throw new Exception();
    }


  }

  public Term getAsJasonTerm(JsonElement element) {
    Term term = null;
    if (element.isJsonPrimitive()) {
      term = new StringTermImpl(element.getAsString());

    } else if (element.isJsonArray()){
      JsonArray array = element.getAsJsonArray();
      ListTerm listTerm = new ListTermImpl();
      for (JsonElement jsonElement : array){
        listTerm.append(getAsJasonTerm(jsonElement));
      }
      term = listTerm;

  } else if (element.isJsonObject()){
      JsonObject object = element.getAsJsonObject();
      MapTerm mapTerm = new MapTermImpl();
      for (Map.Entry<String, JsonElement> entry: object.entrySet()){
        mapTerm.put(new StringTermImpl(entry.getKey()), getAsJasonTerm(entry.getValue()));
      }
      term = mapTerm;

  }
    return term;
  }



}
