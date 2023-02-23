package org.hyperagents.yggdrasil.jason.dlt;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.RDFHandlerWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

public class getAsDLTMessage extends DefaultInternalAction {

  @Override
  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {
    boolean b = false;
    int n = arg.length;
    MapTerm mt = (MapTerm) arg[0];
    Optional<String> tdUrl = Optional.empty();
    Optional<String> affordanceName = Optional.empty();
    Optional<String> agentUri = Optional.empty();
    if (n==3){
      agentUri = Optional.of(((StringTerm) arg[1]).getString());
    }
    if (n==5){
      tdUrl = Optional.of(((StringTerm) arg[1]).getString());
      affordanceName = Optional.of(((StringTerm) arg[2]).getString());

    }
    if (n==6){
      agentUri = Optional.of(((StringTerm) arg[1]).getString());
      tdUrl = Optional.of(((StringTerm) arg[2]).getString());
      affordanceName = Optional.of(((StringTerm) arg[3]).getString());

    }
    try {
      String message = getAsDLTMessage(mt, agentUri, tdUrl, affordanceName);
      un.bind((VarTerm) arg[n-1], new StringTermImpl(message));
      b = true;
    } catch (Exception e){
      e.printStackTrace();
    }
    return b;

  }


  public String getAsDLTMessage(MapTerm mt, Optional<String> agentUri, Optional<String> tdUrl, Optional<String> affordanceName) throws IOException {
    InteractionModel im = new InteractionModel(mt);
    if (agentUri.isPresent()){
      im.setAgentInfo(agentUri.get());
    }
    if (tdUrl.isPresent() && affordanceName.isPresent()){
      im.setTDInfo(tdUrl.get(), affordanceName.get());
    }
    Model m = im.createModel();
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
