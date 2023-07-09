package org.hyperagents.yggdrasil.jason.json;


import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

public class createMapTerm extends DefaultInternalAction {

  @Override
  public Object execute(TransitionSystem ts, Unifier un, final Term[] arg) throws Exception {
    ListTerm attributes = (ListTerm) arg[0];
    ListTerm values = (ListTerm) arg[1];
    MapTerm mt = createMapTerm(attributes, values);
    un.bind((VarTerm) arg[2], mt);
    return true;
  }

  public MapTerm createMapTerm(ListTerm attributes, ListTerm values){
    MapTerm mt = new MapTermImpl();
    int n1 = attributes.size();
    int n2 = values.size();
    if (n1 == n2){
      for (int i = 0; i<n1;i++){
        Term key = attributes.get(i);
        Term value = values.get(i);
        mt.put(key, value);
      }
    }
    return mt;
  }
}
