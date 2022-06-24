package org.hyperagents.yggdrasil.jason.wot;

import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.EventAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Term;
import org.hyperagents.yggdrasil.jason.YAgentArch;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class subscribeEvent extends WotAction{

  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {
    StringTerm tdUriTerm = (StringTerm) arg[0];
    String tdUri = tdUriTerm.getString();
    StringTerm eventTerm = (StringTerm) arg[1];
    String eventName = eventTerm.getString();
    String body = null;
    if (arg.length > 2) {
      StringTerm bodyTerm = (StringTerm) arg[2];
      body = bodyTerm.getString();
    }
    Map<String, String> headers = new Hashtable<>();
    if (ts.getAgArch() instanceof YAgentArch) {
      YAgentArch agArch = (YAgentArch) ts.getAgArch();
      headers = agArch.getHeaders();
    }
    subscribeEvent(tdUri, eventName, headers, body);
    return null;
  }

  public void subscribeEvent(String tdUrl, String affordanceName, Map<String, String> headers, String body){
    try {
      ThingDescription td = TDGraphReader.readFromURL(ThingDescription.TDFormat.RDF_TURTLE, tdUrl);
      Optional<EventAffordance> opEvent = td.getEventByName(affordanceName);
      if (opEvent.isPresent()) {
        EventAffordance event = opEvent.get();
        List<Form> forms = event.getForms();
        if (forms.size()>0) {
          Form form = forms.get(0);
          String method = "POST";
          if (form.getMethodName().isPresent()){
            method = form.getMethodName().get();
          }
          sendHttpRequest(form.getTarget(), method, headers, body);
        } else {
          System.out.println("form is not present");
        }
      } else {
        System.out.println("event is not present");
      }
    } catch(Exception e){
      e.printStackTrace();
    }
  }
}
