package org.hyperagents.yggdrasil.jason.json;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

public class getTermAsJsonString extends DefaultInternalAction {

  @Override
  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {
    Term json = arg[0];
    String jsonString = getAsJson(json);
    VarTerm v = (VarTerm) arg[1];
    un.bind(v, new StringTermImpl(jsonString));

    return null;
  }

  public String getAsJson(Term t){
    String s = "";
    if (t.isMap()){
      MapTerm mt = (MapTerm) t;
      s = "{";
      for (Term key: mt.keys()){
        String keyString = key.toString();
        String valueString = getAsJson(mt.get(key));
        s = s + keyString + ":" + valueString+ ";";
      }
      s = s.substring(0, s.length()-1);
      s = s + "}";

    } else if (t.isList()){
      s = "[";
      ListTerm lt = (ListTerm) t;
      for (Term term: lt){
        s = s + getAsJson(term) + ",";
      }
      s = s.substring(0 , s.length()-1);
      s = s + "]";
    } else if (t.isString()){
      StringTerm st = (StringTerm) t;
      s = t.toString();
    } else if (t.isNumeric()){
      NumberTerm nt = (NumberTerm) t;
      try {
        double d = nt.solve();
        long r = Math.round(d);
        if (d == (double)r) {
          s = String.valueOf(r);
        } else {
          s = String.valueOf(d);
        }
      } catch (Exception e){
        System.err.println("The number is not valid");
      }
    } else if (t.isLiteral()){
      s = t.toString();
      System.out.println("literal is : "+ s);
    }
    return s;
  }

}
