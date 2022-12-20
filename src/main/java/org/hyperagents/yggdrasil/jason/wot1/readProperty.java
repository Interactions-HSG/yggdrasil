package org.hyperagents.yggdrasil.jason.wot1;

import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.affordances.PropertyAffordance;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpRequest;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpResponse;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;
import org.hyperagents.yggdrasil.jason.YAgentArch;

import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;

public class readProperty extends WotAction{

  @Override
  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {
    StringTerm t1 = (StringTerm) arg[0];
    String tdUrl = t1.getString();
    StringTerm t2 = (StringTerm) arg[1];
    String propertyName = t2.getString();
    VarTerm var = (VarTerm) arg[2];
    Map<String, String> headers = new Hashtable<>();
    if (ts.getAgArch() instanceof YAgentArch) {
      YAgentArch agArch = (YAgentArch) ts.getAgArch();
      headers = agArch.getHeaders();
    }
    readProperty(tdUrl, propertyName, headers, var, un,ts);
    return null;

  }

  public void readProperty(String tdUrl, String propertyName, Map<String, String> headers, VarTerm term, Unifier un, TransitionSystem ts){
    tdUrl = tdUrl.replace("\"","");
    try {
      System.out.println("try read property");
      ThingDescription td = TDGraphReader.readFromURL(ThingDescription.TDFormat.RDF_TURTLE, tdUrl);
      System.out.println("td read");
      Optional<PropertyAffordance> opProperty = td.getPropertyByName(propertyName);
      if (opProperty.isPresent()){
        System.out.println("property is present");
        PropertyAffordance property = opProperty.get();
        Optional<Form> opForm = property.getFirstFormForOperationType(TD.readProperty);
        if (opForm.isPresent()){
          System.out.println("form is present");
          Form form = opForm.get();
          TDHttpRequest request = new TDHttpRequest(form, TD.readProperty);
          for (String key: headers.keySet()){
            String value = headers.get(key);
            request.addHeader(key, value);
          }
          TDHttpResponse response = request.execute();
          com.google.gson.JsonObject responseObject = createResponseObject(response);
          System.out.println("response object: "+responseObject);
          bindTermToJson(term, responseObject, un, ts);
        }
      }

    } catch(Exception e){
      e.printStackTrace();
    }

  }

}
