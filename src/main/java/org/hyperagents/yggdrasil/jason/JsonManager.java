package org.hyperagents.yggdrasil.jason;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import jason.asSemantics.Unifier;
import jason.asSyntax.Atom;
import jason.asSyntax.StringTerm;
import jason.asSyntax.StringTermImpl;
import jason.asSyntax.Term;

import java.util.HashMap;

public class JsonManager {

  private HashMap<String, JsonElement> currentJsons;

  private HashMap<JsonElement, Atom> currentJsons_inverse;

  private long id;

  public JsonManager(){
    this.currentJsons = new HashMap<>();
    this.currentJsons_inverse = new HashMap<>();
    this.id = 0;
  }

  public JsonElement getJsonElementFromTerm(Term jsonId){
    if (jsonId.isString()) {
      StringTerm st = (StringTerm) jsonId;
      return currentJsons.get(st.getString());
    }
    else if (jsonId.isAtom()){
      System.out.println("jsonId is atom");
      String s = jsonId.toString();
      System.out.println("all jsons: "+currentJsons.keySet());
      System.out.println("json: "+currentJsons.get(s));
      return currentJsons.get(s);
    }
    return null;
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

  public JsonElement getJSONFromString(String jsonString) throws Exception {
    try {
      JsonElement jsonElement = JsonParser.parseString(jsonString);
      return jsonElement;
    } catch(Exception e){
      throw new Exception();
    }
  }

  public Term getNewJsonId(){
    Term jsonId = new StringTermImpl("JsonElement"+0);
    System.out.println("jsonId: "+jsonId);
    id++;
    return jsonId;
  }

  public boolean registerJson(Term id, JsonElement jsonElement){
    this.currentJsons.put(id.toString(), jsonElement);
    this.currentJsons_inverse.put(jsonElement, new Atom(id.toString()));
    return true;
  }
}
