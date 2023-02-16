package org.hyperagents.yggdrasil.jason.dlt;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.MapTerm;
import jason.asSyntax.StringTermImpl;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.RDFHandlerWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class getAsDLTMessage extends DefaultInternalAction {

  @Override
  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {
    boolean b = false;
    MapTerm mt = (MapTerm) arg[0];
    try {
      String message = getAsDLTMessage(mt);
      un.bind((VarTerm) arg[1], new StringTermImpl(message));
      b = true;
    } catch (Exception e){
      e.printStackTrace();
    }
    return b;

  }


  public String getAsDLTMessage(MapTerm mt) throws IOException {
    Model m = new InteractionModel(mt).createModel();
    OutputStream output = new ByteArrayOutputStream();
    Rio.write(m, output, RDFFormat.JSONLD);
    try {
      output.close();
      return output.toString();
    } catch (Exception e){
      e.printStackTrace();
      throw e;
    }
  }
}
