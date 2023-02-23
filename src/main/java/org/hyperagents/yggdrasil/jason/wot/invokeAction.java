package org.hyperagents.yggdrasil.jason.wot;

import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpRequest;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpResponse;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.MapTerm;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;

import java.util.*;

public class invokeAction extends WoTAction{

  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {
    StringTerm tdUriTerm = (StringTerm) arg[0];
    String tdUrl = tdUriTerm.getString();
    StringTerm actionTerm = (StringTerm) arg[1];
    String actionName = actionTerm.getString();
    String body = "";
    if (arg.length > 3) {
      Term t = arg[2];
      body = getAsJson(t);
      /*boolean b = body.startsWith("\"") && body.endsWith("\"");
      while (b){ //TODO: to check
        body = body.substring(1, body.length()-1);
        System.out.println("current body: "+ body);
        b = body.startsWith("\"") && body.endsWith("\"");
      }*/
    }
    System.out.println("body: "+body);
    Map<String, String> headers = new Hashtable<>();
    if (arg.length > 4) {
      MapTerm headersMap = (MapTerm) arg[3];
      for (Term key : headersMap.keys()) {
        headers.put(cleanString(key.toString()), cleanString(headersMap.get(key).toString()));
      }
    }
    System.out.println("headers: "+headers);
    Map<String, Object> uriVariables = new Hashtable<>();
    if (arg.length > 5) {
      MapTerm uriVariablesMap = (MapTerm) arg[4];
      System.out.println("uri variable map term: "+ uriVariablesMap);
      for (Term key : uriVariablesMap.keys()) {
        StringTerm keyStringTerm = (StringTerm) key;
        String keyString = keyStringTerm.getString();
        System.out.println("key String: "+ keyString);
        Term valueTerm = uriVariablesMap.get(key);
        String valueString = valueTerm.toString();
        if (valueTerm instanceof StringTerm){
          valueString =  ((StringTerm) valueTerm).getString();
        }
        uriVariables.put(keyString, valueString);
      }
    }
    System.out.println("uri variables: "+ uriVariables);
    MapTerm result = invokeAction(tdUrl, actionName, body, headers, uriVariables);
    Term lastTerm = arg[arg.length - 1];
    if (lastTerm.isVar()) {
      VarTerm v = (VarTerm) lastTerm;
      un.bind(v, result);
    }

    return true;
  }

  public MapTerm invokeAction(String tdUrl, String affordanceName, String body, Map<String, String> headers, Map<String, Object> uriVariables){
    try {
      System.out.println("invoke action has body: "+ body);
      ThingDescription td = TDGraphReader.readFromURL(ThingDescription.TDFormat.RDF_TURTLE, tdUrl);
      System.out.println("all actions: "+ td.getActions());
      System.out.println("action name: "+ affordanceName);
      Optional<ActionAffordance> opAction = td.getActionByName(affordanceName);
      if (opAction.isPresent()) {
        ActionAffordance action = opAction.get();
        Optional<Form> opForm = action.getFirstForm();
        if (opForm.isPresent()) {
          Form form = opForm.get();
          TDHttpRequest request = new TDHttpRequest(form, TD.invokeAction);
          if (action.getUriVariables().isPresent()) {
            System.out.println("form target: "+form.getTarget());
            System.out.println("uri variables: "+ uriVariables);
            request = new TDHttpRequest(form, TD.invokeAction, action.getUriVariables().get(), uriVariables);
            System.out.println(request.getTarget());
          }

          for (String key: headers.keySet()){
            System.out.println("header used: "+ key);
            String value = headers.get(key);
            System.out.println("value: "+value);
            request.addHeader(key, value);
          }
          if (body != null){
            //JsonElement element = JsonParser.parseString(body);
            //System.out.println("json element: "+ element);
            Optional<DataSchema> opSchema = action.getInputSchema();
            if (opSchema.isPresent()){
              System.out.println("schema is present");
              if (!headers.containsKey("Content-Type")) {
                request.addHeader("Content-Type", "application/json");
              }
              DataSchema schema = opSchema.get();
              if (Objects.equals(schema.getDatatype(), DataSchema.ARRAY)){
                body = removeQuotes(body);
                JsonElement element = JsonParser.parseString(body);
                List<Object> payload = createArrayPayload(element.getAsJsonArray());
                request.setArrayPayload((ArraySchema) schema, payload);
              } else if (Objects.equals(schema.getDatatype(), DataSchema.OBJECT)){
                body = removeQuotes(body);
                JsonElement element = JsonParser.parseString(body);
                Map<String, Object> payload = createObjectPayload(element.getAsJsonObject());
                request.setObjectPayload((ObjectSchema) schema, payload );
              } else if (Objects.equals(schema.getDatatype(), DataSchema.STRING)){
                JsonElement element = JsonParser.parseString(body);
                request.setPrimitivePayload(schema, element.getAsString());
              } else if (Objects.equals(schema.getDatatype(), DataSchema.NUMBER)){
                body = removeQuotes(body);
                JsonElement element = JsonParser.parseString(body);
                request.setPrimitivePayload(schema, element.getAsDouble());
              } else if (Objects.equals(schema.getDatatype(), DataSchema.INTEGER)){
                body = removeQuotes(body);
                JsonElement element = JsonParser.parseString(body);
                request.setPrimitivePayload(schema, element.getAsLong());
              } else if (Objects.equals(schema.getDatatype(), DataSchema.BOOLEAN)){
                body = removeQuotes(body);
                JsonElement element = JsonParser.parseString(body);
                request.setPrimitivePayload(schema, element.getAsBoolean());
              }
            } else {
              System.out.println("schema is not present");
              request.setPrimitivePayload(new StringSchema.Builder().build(), body);
              System.out.println("payload added");
            }

          }
          TDHttpResponse response = request.execute();
          //return createResponseObject(response);
          String methodName = "POST";
            if (form.getMethodName().isPresent()){
              methodName = form.getMethodName().get();
            }
          return createResponseObject(form.getTarget(), methodName , headers, body, response );
        } else {
          System.out.println("form is not present");
          return null;
        }
      } else {
        System.out.println("action is not present");
        return null;
      }
    } catch(Exception e){
      e.printStackTrace();
    }
    return null;
  }

  public String removeQuotes(String body){
    boolean b = body.startsWith("\"") && body.endsWith("\"");
    while (b){ //TODO: to check
      body = body.substring(1, body.length()-1);
      System.out.println("current body: "+ body);
      b = body.startsWith("\"") && body.endsWith("\"");
    }
    return body;
  }




}
