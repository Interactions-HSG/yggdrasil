package org.hyperagents.yggdrasil.jason.math;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;


public class floor extends DefaultInternalAction {

  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {
    NumberTerm term = (NumberTerm) arg[0];
    try {
      double number = term.solve();
      double n = Math.floor(number);
      VarTerm v = (VarTerm) arg[1];
      un.bind(v, new NumberTermImpl(n));

      return true;

    } catch (Exception e) {
      return false;
    }
  }

}
