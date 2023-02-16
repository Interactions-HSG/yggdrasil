package org.hyperagents.yggdrasil.jason.json;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;

public class getTermAsJson extends DefaultInternalAction {

  @Override
  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {
    Term json = arg[0];
    String jsonString = getAsJson(json);
    VarTerm v = (VarTerm) arg[1];
    un.bind(v, new StringTermImpl(jsonString));

    return true;
  }

  public String getAsJson(Term t){
    StringBuilder s = new StringBuilder();
    if (t.isMap()){
      MapTerm mt = (MapTerm) t;
      s = new StringBuilder("{");
      for (Term key: mt.keys()){
        String keyString = key.toString();
        String valueString = getAsJson(mt.get(key));
        s.append(keyString).append(":").append(valueString).append(";");
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

}
