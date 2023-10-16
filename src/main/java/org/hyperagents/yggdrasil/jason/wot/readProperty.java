package org.hyperagents.yggdrasil.jason.wot;

import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.affordances.PropertyAffordance;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpRequest;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpResponse;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;
import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.MapTerm;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class readProperty extends WoTAction {

  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {
    StringTerm tdUriTerm = (StringTerm) arg[0];
    String tdUrl = tdUriTerm.getString();
    StringTerm propertyTerm = (StringTerm) arg[1];
    String propertyName = propertyTerm.getString();
    Map<String, String> headers = new Hashtable<>();
    if (arg.length > 3) {
      MapTerm headersMap = (MapTerm) arg[2];
      for (Term key : headersMap.keys()) {
        headers.put(key.toString(), headersMap.get(key).toString());
      }
    }
    Map<String, Object> uriVariables = new Hashtable<>();
    if (arg.length > 4) {
      MapTerm uriVariablesMap = (MapTerm) arg[3];
      System.out.println("uri variable map term: "+ uriVariablesMap);
      for (Term key : uriVariablesMap.keys()) {
        StringTerm keyStringTerm = (StringTerm) key;
        String keyString = keyStringTerm.getString();
        System.out.println("key String uri variable: "+ keyString);
        Term valueTerm = uriVariablesMap.get(key);
        String valueString = valueTerm.toString();
        if (valueTerm instanceof StringTerm){
          valueString =  ((StringTerm) valueTerm).getString();
          System.out.println("value string uri variable: "+valueString);
        }
        uriVariables.put(keyString, valueString);
      }
    }
    System.out.println("uri variables: "+ uriVariables);
    MapTerm result = readProperty(tdUrl, propertyName, headers, uriVariables);
    Term lastTerm = arg[arg.length - 1];
    if (lastTerm.isVar()) {
      VarTerm v = (VarTerm) lastTerm;
      un.bind(v, result);
    }

    return true;
  }

  public MapTerm readProperty(String tdUrl, String affordanceName, Map<String, String> headers, Map<String, Object> uriVariables){
    try {
      ThingDescription td = TDGraphReader.readFromURL(ThingDescription.TDFormat.RDF_TURTLE, tdUrl);
      Optional<PropertyAffordance> opProperty = td.getPropertyByName(affordanceName);
      if (opProperty.isPresent()) {
        PropertyAffordance property = opProperty.get();
        List<Form> formList= property.getForms();
        if (formList.size()>0) {
          Form form = formList.get(0);
          TDHttpRequest request = new TDHttpRequest(form, TD.readProperty);
          if (property.getUriVariables().isPresent()) {
            System.out.println("form target: "+form.getTarget());
            request = new TDHttpRequest(form, TD.readProperty, property.getUriVariables().get(), uriVariables);
            System.out.println("target url: "+request.getTarget());
          }

          for (String key: headers.keySet()){
            String value = headers.get(key);
            request.addHeader(key, value);
          }
          TDHttpResponse response = request.execute();
          String methodName = "GET";
          if (form.getMethodName().isPresent()){
            methodName = form.getMethodName().get();
          }
          return createResponseObject(form.getTarget(), methodName , headers, "", response );
        } else {
          System.out.println("form is not present");
          return null;
        }
      } else {
        System.out.println("property is not present");
        return null;
      }
    } catch(Exception e){
      e.printStackTrace();
    }
    return null;
  }
}
