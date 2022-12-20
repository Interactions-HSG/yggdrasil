package org.hyperagents.yggdrasil.jason.wot1;

import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpRequest;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpResponse;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ListTerm;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;
import org.hyperagents.yggdrasil.jason.YAgentArch;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class invokeAction extends WotAction {

  @Override
  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {
    StringTerm t1 = (StringTerm) arg[0];
    String tdUrl = t1.getString();
    StringTerm t2 = (StringTerm) arg[1];
    String actionName = t2.getString();
    Map<String, String> headers = new Hashtable<>();
    if (ts.getAgArch() instanceof YAgentArch) {
      YAgentArch agArch = (YAgentArch) ts.getAgArch();
      headers = agArch.getHeaders();
    }
    String body = null;
    if (arg.length > 2) {
      Term t3 = arg[2];
      if (t3.isString()) {
        StringTerm st = (StringTerm) t3;
        body =  st.getString();
        System.out.println("body: "+body);
      }
    }
    if (arg.length == 4){
      VarTerm var = (VarTerm) arg[3];
      invokeAction(tdUrl, actionName, headers, body, var, un ,ts);
    }
    else if (arg.length == 6){
      ListTerm uriVariableNames = (ListTerm) arg[3];
      ListTerm uriVariableValues = (ListTerm) arg[4];
      VarTerm var = (VarTerm) arg[5];
      invokeAction(tdUrl, actionName, headers, body, uriVariableNames, uriVariableValues, var, un, ts);
    } else {
      invokeAction(tdUrl, actionName, headers, body, un, ts);
    }
    return null;


  }

  public void invokeAction(String tdUrl, String affordanceName, Map<String, String> headers, String body, VarTerm term, Unifier un, TransitionSystem ts){
    try {
      ThingDescription td = TDGraphReader.readFromURL(ThingDescription.TDFormat.RDF_TURTLE, tdUrl);
      System.out.println("td received");
      System.out.println("td: "+ new TDGraphWriter(td).write());
      System.out.println("number of actions: "+td.getActions().size());
      td.getActions().forEach(a -> System.out.println(a));
      Optional<ActionAffordance> opAction = td.getActionByName(affordanceName);
      if (opAction.isPresent()) {
        System.out.println("action is present");
        ActionAffordance action = opAction.get();
        Optional<Form> opForm = action.getFirstForm();
        if (opForm.isPresent()) {
          System.out.println("form is present");
          Form form = opForm.get();
          TDHttpRequest request = new TDHttpRequest(form, TD.invokeAction);
          System.out.println("request target: "+request.getTarget());

          for (String key: headers.keySet()){
            String value = headers.get(key);
            request.addHeader(key, value);
          }
          if (body != null){
            JsonElement element = JsonParser.parseString(body);
            Optional<DataSchema> opSchema = action.getInputSchema();
            if (opSchema.isPresent()){
              request.addHeader("Content-Type", "application/json");
              System.out.println("schema is present");
              DataSchema schema = opSchema.get();
              if (schema.getDatatype() == "array" && element.isJsonArray()){
                List<Object> payload = createArrayPayload(element.getAsJsonArray());
                request.setArrayPayload((ArraySchema) schema, payload);
              } else if (schema.getDatatype() == "object" && element.isJsonObject()){
                Map<String, Object> payload = createObjectPayload(element.getAsJsonObject());
                request.setObjectPayload((ObjectSchema) schema, payload );
              } else if (schema.getDatatype() == "string"){
                request.setPrimitivePayload(schema, element.getAsString());
              } else if (schema.getDatatype() == "number"){
                request.setPrimitivePayload(schema, element.getAsDouble());
              } else if (schema.getDatatype() == "integer"){
                request.setPrimitivePayload(schema, element.getAsLong());
              } else if (schema.getDatatype() == "boolean"){
                request.setPrimitivePayload(schema, element.getAsBoolean());
              }
            }
            System.out.println("request body: "+request.getPayloadAsString());

          }
          TDHttpResponse response = request.execute();
          com.google.gson.JsonObject responseObject = createResponseObject(response);
          bindTermToJson(term, responseObject, un, ts);
          //Unifier u =  getTS().getC().getSelectedIntention().peek().getUnif();
          //u.bind(term, new StringTermImpl(response.getPayloadAsString()));
        } else {
          System.out.println("form is not present");
        }
      } else {
        System.out.println("action is not present");
      }
    } catch(Exception e){
      e.printStackTrace();
    }
  }

  public void invokeAction(String tdUrl, String affordanceName, Map<String, String> headers, String body, Unifier un, TransitionSystem ts){
    try {
      ThingDescription td = TDGraphReader.readFromURL(ThingDescription.TDFormat.RDF_TURTLE, tdUrl);
      System.out.println("td received");
      System.out.println("td: "+ new TDGraphWriter(td).write());
      System.out.println("number of actions: "+td.getActions().size());
      td.getActions().forEach(a -> System.out.println(a));
      Optional<ActionAffordance> opAction = td.getActionByName(affordanceName);
      if (opAction.isPresent()) {
        System.out.println("action is present");
        ActionAffordance action = opAction.get();
        Optional<Form> opForm = action.getFirstForm();
        if (opForm.isPresent()) {
          System.out.println("form is present");
          Form form = opForm.get();
          TDHttpRequest request = new TDHttpRequest(form, TD.invokeAction);
          System.out.println("request target: "+request.getTarget());

          for (String key: headers.keySet()){
            String value = headers.get(key);
            request.addHeader(key, value);
          }
          if (body != null){
            JsonElement element = JsonParser.parseString(body);
            Optional<DataSchema> opSchema = action.getInputSchema();
            if (opSchema.isPresent()){
              request.addHeader("Content-Type", "application/json");
              System.out.println("schema is present");
              DataSchema schema = opSchema.get();
              if (schema.getDatatype() == "array" && element.isJsonArray()){
                List<Object> payload = createArrayPayload(element.getAsJsonArray());
                request.setArrayPayload((ArraySchema) schema, payload);
              } else if (schema.getDatatype() == "object" && element.isJsonObject()){
                Map<String, Object> payload = createObjectPayload(element.getAsJsonObject());
                request.setObjectPayload((ObjectSchema) schema, payload );
              } else if (schema.getDatatype() == "string"){
                request.setPrimitivePayload(schema, element.getAsString());
              } else if (schema.getDatatype() == "number"){
                request.setPrimitivePayload(schema, element.getAsDouble());
              } else if (schema.getDatatype() == "integer"){
                request.setPrimitivePayload(schema, element.getAsLong());
              } else if (schema.getDatatype() == "boolean"){
                request.setPrimitivePayload(schema, element.getAsBoolean());
              }
            }
            System.out.println("request body: "+request.getPayloadAsString());

          }
          TDHttpResponse response = request.execute();
          com.google.gson.JsonObject responseObject = createResponseObject(response);
          //Unifier u =  getTS().getC().getSelectedIntention().peek().getUnif();
          //u.bind(term, new StringTermImpl(response.getPayloadAsString()));
        } else {
          System.out.println("form is not present");
        }
      } else {
        System.out.println("action is not present");
      }
    } catch(Exception e){
      e.printStackTrace();
    }
  }

  public void invokeAction(String tdUrl, String affordanceName, Map<String, String> headers, String body, ListTerm uriVariableNames, ListTerm uriVariableValues, VarTerm term, Unifier un, TransitionSystem ts){
    try {
      ThingDescription td = TDGraphReader.readFromURL(ThingDescription.TDFormat.RDF_TURTLE, tdUrl);
      System.out.println("td received");
      System.out.println("td: "+ new TDGraphWriter(td).write());
      System.out.println("number of actions: "+td.getActions().size());
      td.getActions().forEach(a -> System.out.println(a));
      Optional<ActionAffordance> opAction = td.getActionByName(affordanceName);
      if (opAction.isPresent()) {
        System.out.println("action is present");
        ActionAffordance action = opAction.get();
        Optional<Form> opForm = action.getFirstForm();
        if (opForm.isPresent()) {
          System.out.println("form is present");
          Form form = opForm.get();
          TDHttpRequest request = new TDHttpRequest(form, TD.invokeAction);
          if (action.getUriVariables().isPresent()) {
            Map<String, DataSchema> uriVariables = action.getUriVariables().get();
            Map<String, Object> values = new Hashtable<>();
            int n = uriVariableNames.size();
            int m = uriVariableValues.size();
            if (n==m){
              for (int i = 0; i <n; i++){
                StringTerm name = (StringTerm) uriVariableNames.get(i);
                System.out.println("name: "+name);
                StringTerm value = (StringTerm) uriVariableValues.get(i);
                System.out.println("value: "+value);
                values.put(name.getString(), value.getString());
              }
            }
            System.out.println("uri variables: "+uriVariables);
            System.out.println("values: "+values);
            request = new TDHttpRequest(form, TD.invokeAction, action.getUriVariables().get(), values);
            System.out.println(request.getTarget());
          }
          System.out.println("request target: "+request.getTarget());

          for (String key: headers.keySet()){
            String value = headers.get(key);
            request.addHeader(key, value);
          }
          if (body != null){
            JsonElement element = JsonParser.parseString(body);
            Optional<DataSchema> opSchema = action.getInputSchema();
            if (opSchema.isPresent()){
              request.addHeader("Content-Type", "application/json");
              System.out.println("schema is present");
              DataSchema schema = opSchema.get();
              if (schema.getDatatype() == "array" && element.isJsonArray()){
                List<Object> payload = createArrayPayload(element.getAsJsonArray());
                request.setArrayPayload((ArraySchema) schema, payload);
              } else if (schema.getDatatype() == "object" && element.isJsonObject()){
                Map<String, Object> payload = createObjectPayload(element.getAsJsonObject());
                request.setObjectPayload((ObjectSchema) schema, payload );
              } else if (schema.getDatatype() == "string"){
                request.setPrimitivePayload(schema, element.getAsString());
              } else if (schema.getDatatype() == "number"){
                request.setPrimitivePayload(schema, element.getAsDouble());
              } else if (schema.getDatatype() == "integer"){
                request.setPrimitivePayload(schema, element.getAsLong());
              } else if (schema.getDatatype() == "boolean"){
                request.setPrimitivePayload(schema, element.getAsBoolean());
              }
            }
            System.out.println("request body: "+request.getPayloadAsString());

          }
          TDHttpResponse response = request.execute();
          com.google.gson.JsonObject responseObject = createResponseObject(response);
          bindTermToJson(term, responseObject, un, ts);
          //Unifier u =  getTS().getC().getSelectedIntention().peek().getUnif();
          //u.bind(term, new StringTermImpl(response.getPayloadAsString()));
        } else {
          System.out.println("form is not present");
        }
      } else {
        System.out.println("action is not present");
      }
    } catch(Exception e){
      e.printStackTrace();
    }
  }
}
