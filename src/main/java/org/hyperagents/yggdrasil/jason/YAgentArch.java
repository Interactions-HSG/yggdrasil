package org.hyperagents.yggdrasil.jason;

import cartago.*;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.EventAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.affordances.PropertyAffordance;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpRequest;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpResponse;
import ch.unisg.ics.interactions.wot.td.clients.UriTemplate;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import ch.unisg.ics.interactions.wot.td.schemas.*;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import jason.architecture.AgArch;
import jason.asSemantics.*;
import jason.asSyntax.*;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.hyperagents.yggdrasil.cartago.CartagoVerticle;
import org.hyperagents.yggdrasil.cartago.NotificationCallback;
import org.hyperagents.yggdrasil.cartago.WorkspaceRegistry;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;
import org.hyperagents.yggdrasil.store.RdfStore;
import org.hyperagents.yggdrasil.websub.NotificationSubscriberRegistry;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class YAgentArch extends AgArch {

  Vertx vertx;
  int messageId;
  HttpClient client = HttpClients.createDefault();
  Map<String, String> headers;

  public YAgentArch(Vertx vertx){

    this.vertx = vertx;
    this.headers = new Hashtable<>();
    headers.put("X-Agent-WebID", this.getAgName());
  }

  public YAgentArch(){
    System.out.println("creating YAgentArch");
    this.vertx = VertxRegistry.getInstance().getVertx();
    System.out.println("vertx: "+vertx);
    //this.vertx = new VertxFactoryImpl().vertx();
    messageId = 0;
    this.headers = new Hashtable<>();
    //headers.put("X-Agent-WebID", this.getAgName());
  }


  @Override
  public void act(ActionExec actionExec) {
    System.out.println("act");
    System.out.println("perform action: "+actionExec.getActionTerm());
    String agentName = getAgName();
    System.out.println("agent name: " + agentName);
    Intention currentIntention = getTS().getC().getSelectedIntention();
    Unifier un = currentIntention.peek().getUnif();
    un = actionExec.getIntention().peek().getUnif();
    Structure action = actionExec.getActionTerm();

    ListTerm lt = action.getAnnots();
    if (lt != null) {
      Iterator<Term> it = lt.iterator();
      while (it.hasNext()) {
        Term annot = it.next();
      }
    }
    String func = action.getFunctor();
    List<Term> terms = action.getTerms();
    if (func.equals("createWorkspace")) {
      String workspaceName = terms.get(0).toString();
      createWorkspace(workspaceName);
      System.out.println("workspace created");
    } else if (func.equals("createSubWorkspace")) {
      String workspaceName = terms.get(0).toString();
      String subWorkspaceName = terms.get(1).toString();
      createSubWorkspace(workspaceName, subWorkspaceName);
      System.out.println("sub workspace created");
    } else if (func.equals("makeArtifact")) {
      String workspaceName = terms.get(0).toString();
      String artifactName = terms.get(1).toString();
      String artifactInit = terms.get(2).toString();
      makeArtifact(workspaceName, artifactName, artifactInit);
    } else if (func.equals("joinWorkspace")) {
      String workspaceName = terms.get(0).toString();
      joinWorkspace(workspaceName);

    } else if (func.equals("leaveWorkspace")) {
      String workspaceName = terms.get(0).toString();
      leaveWorkspace(workspaceName);
    } else if (func.equals("focus")) {
      System.out.println("start focus");
      String workspaceName = terms.get(0).toString();
      String artifactName = terms.get(1).toString();
      focus(workspaceName, artifactName);
      System.out.println("end focus");

    } else if (func.equals("stopFocus")) {

    } else if (func.equals("setValue")){
      Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
      VarTerm v =  (VarTerm) terms.get(0);
      System.out.println("variable: "+v);
      Term t = terms.get(1);
      System.out.println("value: "+t);
      u.bind(v,t);

    }

    else if (func.equals("invokeAction")) {
      StringTerm tdUriTerm = (StringTerm) terms.get(0);
      String tdUri = tdUriTerm.getString();
      StringTerm actionTerm = (StringTerm) terms.get(1);
      String actionName = actionTerm.getString();
      String body = null;
      if (terms.size() > 2) {
        Term t = terms.get(2);
        if (t.isString()) {
          StringTerm st = (StringTerm) terms.get(2);
          body =  st.getString();
          System.out.println("body: "+body);
        }
      }
      if (terms.size() == 4){
        VarTerm var = (VarTerm) terms.get(3);
        invokeAction(tdUri, actionName, headers, body, var);
      }
      else if (terms.size() == 6){
        ListTerm uriVariableNames = (ListTerm) terms.get(3);
        ListTerm uriVariableValues = (ListTerm) terms.get(4);
        System.out.println("return term: "+ terms.get(5));
        VarTerm var = (VarTerm) terms.get(5);
        invokeAction(tdUri, actionName, headers, body, uriVariableNames, uriVariableValues, var);
      } else {
        invokeAction(tdUri, actionName, headers, body);
      }
    } else if (func.equals("subscribeEvent")) {
      System.out.println(terms.get(0));
      StringTerm tdUriTerm = (StringTerm) terms.get(0);
      System.out.println("td term retrieved");
      String tdUri = tdUriTerm.getString();
      StringTerm eventTerm = (StringTerm) terms.get(1);
      System.out.println("event name retrieved");
      String eventName = eventTerm.getString();
      String body = null;
      if (terms.size() > 2) {
        StringTerm bodyTerm = (StringTerm) terms.get(2);
        System.out.println("body term retrieved");
        body = bodyTerm.getString();
      }
      subscribeEvent(tdUri, eventName, headers, body);
    } else if (func.equals("readProperty")){
      StringTerm tdUriTerm = (StringTerm) terms.get(0);
      String tdUri = tdUriTerm.getString();
      StringTerm propertyTerm = (StringTerm) terms.get(1);
      String propertyName =propertyTerm.getString();
      VarTerm term =  (VarTerm) terms.get(2);
      readProperty(tdUri, propertyName, headers, term);

    }
    else if (func.equals("addHeader")){
      String key = terms.get(0).toString();
      String value = terms.get(1).toString();
      headers.put(key, value);
    }
    else if (func.equals("removeHeader")){
      String key = terms.get(0).toString();
      headers.remove(key);
    }

    else if (func.equals("sendHttpRequest")) {
      StringTerm urlTerm = (StringTerm) terms.get(0);
      String url = urlTerm.getString();
      System.out.println("url: " + url);
      StringTerm methodTerm = (StringTerm) terms.get(1);
      String method = methodTerm.toString();
      System.out.println("method: " + method);
      String body = null;
      if (terms.size() > 2) {
        StringTerm bodyTerm = (StringTerm) terms.get(2);
        body = bodyTerm.getString();
      }
      com.google.gson.JsonObject o = sendHttpRequest(url, method, headers, body);
      System.out.println("return object: "+o);
      if (terms.size()>3){
        Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
        bindTermToJson(terms.get(3), o);
      }
    } else if (func.equals("printJson")) {
      System.out.println("printJson");
      Term jsonId = terms.get(0);
      System.out.println("json id: "+jsonId);
      printJSON(jsonId);
      } else if (func.equals("makeJson")){
      ListTerm attributeList = (ListTerm) terms.get(0);
      ListTerm valueList = (ListTerm) terms.get(1);
      VarTerm jsonId = (VarTerm) terms.get(2);
      Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
      createJsonObject(u, attributeList, valueList, jsonId);
    } else if (func.equals("hasAttribute")){
      Term jsonId = terms.get(0);
      StringTerm attributeTerm = (StringTerm) terms.get(1);
      String attribute = attributeTerm.getString();
      boolean b = hasAttribute(jsonId, attribute);
      Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
      VarTerm v = (VarTerm) terms.get(2);
      Term t = Literal.LFalse;
      if (b){
        t = Literal.LTrue;
      }
      u.bind(v,t);
    } else if (func.equals("isValid")){

      Term jsonId = terms.get(0);
      VarTerm bVar = (VarTerm) terms.get(1);
      boolean b = isValid(jsonId);
      Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
      if (b) {
        u.bind(bVar, Literal.LTrue);
      } else {
        u.bind(bVar, Literal.LFalse);
      }
    }  else if (func.equals("isInformation")){
      Term jsonId = terms.get(0);
      VarTerm bVar = (VarTerm) terms.get(1);
      boolean b = isInformation(jsonId);
      Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
      if (b) {
        u.bind(bVar, Literal.LTrue);
      } else {
        u.bind(bVar, Literal.LFalse);
      }
    } else if (func.equals("isRedirection")){
      Term jsonId = terms.get(0);
      VarTerm bVar = (VarTerm) terms.get(1);
      boolean b = isRedirection(jsonId);
      Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
      if (b) {
        u.bind(bVar, Literal.LTrue);
      } else {
        u.bind(bVar, Literal.LFalse);
      }
    } else if (func.equals("isClientError")){
      Term jsonId = terms.get(0);
      VarTerm bVar = (VarTerm) terms.get(1);
      boolean b = isClientError(jsonId);
      Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
      if (b) {
        u.bind(bVar, Literal.LTrue);
      } else {
        u.bind(bVar, Literal.LFalse);
      }
    } else if (func.equals("isServerError")){
      Term jsonId = terms.get(0);
      VarTerm bVar = (VarTerm) terms.get(1);
      boolean b = isServerError(jsonId);
      Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
      if (b) {
        u.bind(bVar, Literal.LTrue);
      } else {
        u.bind(bVar, Literal.LFalse);
      }
    }

    else if (func.equals("getJsonAsString")){
      System.out.println("term 0: "+terms.get(0));
      Term jsonId = terms.get(0);
      System.out.println("jsonId: "+jsonId);
      StringTerm str = getAsStringTerm(jsonId);
      System.out.println("json as string: "+str);
      un.bind((VarTerm) terms.get(1), str);

    } else if (func.equals("getStringAsJson")){
      StringTerm st = (StringTerm) terms.get(0);
      Term jsonId = terms.get(1);
      JSONLibrary library = JSONLibrary.getInstance();
      Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
      try {
        library.new_json(u, st.getString(), jsonId);
      } catch(Exception e){
        e.printStackTrace();
      }
    }

    else if (func.equals("getStringFromJson")){
      Term jsonId = terms.get(0);
      System.out.println("jsonId: "+jsonId);
      String attribute = ((StringTerm) terms.get(1)).getString();
      System.out.println("attribute: "+attribute);
      String str = getStringFromJson(jsonId, attribute);
      System.out.println("string: "+str);
      StringTerm value = new StringTermImpl(str);
      un.bind((VarTerm) terms.get(2), value);

    }  else if (func.equals("getNumberFromJson")){
      Term jsonId = terms.get(0);
      String attribute = ((StringTerm) terms.get(1)).getString();
      NumberTerm value = new NumberTermImpl(getNumberFromJson(jsonId, attribute));
      un.bind((VarTerm) terms.get(2), value);

    } else if (func.equals("testUriVariables")){
      String uriTemplate = "http://example.org/{?a,b}";
      UriTemplate template = new UriTemplate(uriTemplate);
      Map<String, DataSchema> uriVariables = new Hashtable<>();
      uriVariables.put("a", new StringSchema.Builder().build());
      uriVariables.put("b", new StringSchema.Builder().build());
      Map<String, Object> values = new Hashtable<>();
      values.put("a", "abc");
      values.put("b", "gh");
      String uri = template.createUri(uriVariables, values);
      System.out.println(uri);
      System.out.println("second test uri");
      Form form = new Form.Builder("http://example.org{?a,b}").build();
      TDHttpRequest request = new TDHttpRequest(form, TD.invokeAction, uriVariables, values);
      System.out.println("target uri: "+request.getTarget());

    } else if (func.equals("getBody")){
      Term jsonId = terms.get(0);
      String body = getBody(jsonId);
      VarTerm v = (VarTerm ) terms.get(1);
      Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
      u.bind(v, new StringTermImpl(body));
    } else if (func.equals("getCurrentTime")){
      VarTerm var = (VarTerm) terms.get(0);
      Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
      String timeStamp = getCurrentTimeStamp();
      System.out.println("current time stamp: "+timeStamp);
      u.bind(var, new StringTermImpl(timeStamp));
    }

      System.out.println("end method act");
      actionExec.setResult(true);
      super.actionExecuted(actionExec);
    }



  @Override
  public Collection<Literal> perceive(){
    try {
      AgentRegistry registry = AgentRegistry.getInstance();
      AgentNotificationCallback callback = registry.getAgentCallback(this.getAgName());
      if (!callback.isEmpty()) {
        String notification = callback.retrieveNotification();
        System.out.println("notification received: " + notification);
        Literal belief = Literal.parseLiteral(notification);
        this.getTS().getAg().addBel(belief);
      }
        String agentName = this.getAgName();
        AgentMessageCallback messageCallback = registry.getAgentMessageCallback(agentName);
        if (messageCallback.hasNewMessage()) {
          System.out.println("agent "+ this.getAgName()+ " has new message");
          String message = messageCallback.retrieveMessage();
          Literal messageBelief = new LiteralImpl("new_message");
          Term id = getNewMessageId();
          JSONLibrary jsonLibrary = JSONLibrary.getInstance();
          JsonElement jsonElement = jsonLibrary.getJSONFromString(message);
          Term jsonTerm = jsonLibrary.getNewJsonId();
          jsonLibrary.registerJson(jsonTerm, jsonElement);
          messageBelief.addTerm(id);
          messageBelief.addTerm(jsonTerm);
          System.out.println("message belief: "+messageBelief);
          this.getTS().getAg().addBel(messageBelief);
          messageCallback.noNewMessage();
        }
      } catch(Exception e){
      e.printStackTrace();
    }


    return super.perceive();
  }

  private Term getNewMessageId(){
    Term messageTermId = new StringTermImpl("Message"+this.messageId);
    this.messageId ++;
    return messageTermId;
  }


  boolean isEnvironmentOperation(String o){
    return o.equals("joinWorkspace") ||
      o.equals("createWorkspace")||
      o.equals("createSubWorkspace")||
      //o.equals("lookupArtifact") ||
      o.equals("makeArtifact")||
      o.equals("focus")||
      //o.equals("focusWhenAvailable")||
      o.equals("leaveWorkspace");
  }

  //Syntax for the operations
  //joinWorkspace(String workspaceName)
  //createWorkspace(String workspaceName)
  //createSubWorkspace(String workspaceName, String subWorkspaceName)
  //makeArtifact(String workspaceName, String artifactName)
  //focus(String workspaceName, String artifactname)
  //leaveWorkspace(String workspaceName)




  public void createWorkspace1(String workspaceName){
    Map<String, String> headers = new Hashtable<>();
    headers.put("X-Agent-WebID", this.getAgName());
    headers.put("Slug", workspaceName);
    sendHttpRequest("http://localhost:8080/workspaces/", "POST", headers, null);
  }

  public void createWorkspace2(String workspaceName){
    DeliveryOptions options = new DeliveryOptions()
      .addHeader(HttpEntityHandler.REQUEST_METHOD, CartagoVerticle.CREATE_WORKSPACE)
      .addHeader(CartagoVerticle.AGENT_ID, this.getAgName())
      .addHeader(CartagoVerticle.WORKSPACE_NAME, workspaceName);
    vertx.eventBus().request(CartagoVerticle.BUS_ADDRESS, "", options, r -> {
      if (r.succeeded()){
        String description = r.result().body().toString();
        System.out.println("description: "+description);
        DeliveryOptions rdfOptions = new DeliveryOptions()
          .addHeader(HttpEntityHandler.REQUEST_METHOD, RdfStore.CREATE_ENTITY)
          .addHeader(HttpEntityHandler.REQUEST_URI, "http://localhost:8080/workspaces/")
          .addHeader(HttpEntityHandler.ENTITY_URI_HINT, workspaceName);
        vertx.eventBus().request(RdfStore.BUS_ADDRESS, description, rdfOptions, r2 -> {
          if (r2.succeeded()){
            System.out.println("workspace registered");
          } else {
            System.out.println("workspace could not be registered");
          }
        });
      } else {
        System.out.println("create workspace failed");
      }
    });
    System.out.println("create workspace 7 finished");

  }


  public void createWorkspace(String workspaceName){
    System.out.println("is creating workspace");
    System.out.println("agent: "+this.getAgName());
    createWorkspace1(workspaceName);
  }


  public void createSubWorkspace(String workspaceName, String subWorkspaceName){
    String uri = "http://localhost:8080/workspaces/" + workspaceName +"/sub";
    Map<String, String> headers = new Hashtable<>();
    headers.put("X-Agent-WebID", this.getAgName());
    JsonObject object = new JsonObject();
    object.put("name", subWorkspaceName);
    String body = object.encode();
    System.out.println("body: "+body);

    sendHttpRequest(uri, "POST", headers, body);

  }





  public void makeArtifact(String workspaceName, String artifactName, String artifactClass){
    String uri = "http://localhost:8080/workspaces/"+workspaceName+"/artifacts/";
    Map<String, String> headers = new Hashtable<>();
    headers.put("X-Agent-WebID", this.getAgName());
    headers.put("Content-Type", "application/json");
    artifactName = artifactName.replace("\"", "");
    artifactClass = artifactClass.replace("\"","");
    JsonObject object = new JsonObject();
    object.put("artifactName", artifactName);
    object.put("artifactClass", artifactClass);
    String artifactInit = object.encode();
    sendHttpRequest(uri, "POST", headers, artifactInit);
  }



  public void joinWorkspace(String workspaceName){
    String uri = "http://localhost:8080/workspaces/"+workspaceName+"/join";
    Map<String, String> headers = new Hashtable<>();
    headers.put("X-Agent-WebID", this.getAgName());
    String response = sendHttpRequest(uri, "PUT", headers, null).get("body").getAsString();
    try {
      System.out.println("body description: "+response);
      ThingDescription td = TDGraphReader.readFromString(ThingDescription.TDFormat.RDF_TURTLE, response);
      Optional<String> opName = td.getThingURI();
      if (opName.isPresent()){
        String bodyName = opName.get();
        System.out.println("body name: "+bodyName);
        AgentRegistry.getInstance().addBody(this.getAgName(), workspaceName, bodyName );
      }
    } catch(Exception e){
      e.printStackTrace();
    }
  }




  public void leaveWorkspace(String workspaceName){
    String uri = "http://localhost:8080/workspaces/"+workspaceName+"/leave";
    Map<String, String> headers = new Hashtable<>();
    headers.put("X-Agent-WebID", this.getAgName());
    sendHttpRequest(uri, "DELETE", headers, null);
    AgentRegistry.getInstance().removeBody(this.getAgName(), workspaceName);
  }

  public void focus(String workspaceName, String artifactName){
    try {
      String bodyName = AgentRegistry.getInstance().getBody(this.getAgName(), workspaceName);
      String focusUri = bodyName+"/focus";
      System.out.println("body name: "+bodyName);
      Map<String, String> headers = new Hashtable<>();
      headers.put("X-Agent-WebID", this.getAgName());
      headers.put("Content-Type", "application/json");
      String body = "[\""+artifactName+"\"]";
      System.out.println("focus body: "+body);
      sendHttpRequest(focusUri, "PUT", headers, body);
      String artifactIRI = "http://localhost:8080/workspaces/"+workspaceName+"/artifacts/"+artifactName;
      System.out.println("artifact IRI: "+artifactIRI);
      NotificationSubscriberRegistry.getInstance().addCallbackIRI(artifactIRI, this.getAgName());

    } catch(Exception e){
      e.printStackTrace();
    }
  }

  public void stopFocus(String workspaceName, String artifactName){
    try {
      String bodyName = AgentRegistry.getInstance().getBody(this.getAgName(), workspaceName);
      String focusUri = bodyName+"/stopFocus";
      System.out.println("body name: "+bodyName);
      Map<String, String> headers = new Hashtable<>();
      headers.put("X-Agent-WebID", this.getAgName());
      headers.put("Content-Type", "application/json");
      String body = "[\""+artifactName+"\"]";
      System.out.println("focus body: "+body);
      sendHttpRequest(focusUri, "PUT", headers, body);

    } catch(Exception e){
      e.printStackTrace();
    }
  }







  public void invokeAction(String tdUrl, String affordanceName, Map<String, String> headers, String body, VarTerm term){
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
        bindTermToJson(term, responseObject);
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



  public void invokeAction(String tdUrl, String affordanceName, Map<String, String> headers, String body, ListTerm uriVariableNames, ListTerm uriVariableValues, VarTerm term){
    try {
      ThingDescription td = TDGraphReader.readFromURL(ThingDescription.TDFormat.RDF_TURTLE, tdUrl);
      Optional<ActionAffordance> opAction = td.getActionByName(affordanceName);
      System.out.println("number of actions: "+td.getActions().size());
      td.getActions().forEach(a -> System.out.println(a));
      if (opAction.isPresent()) {
        ActionAffordance action = opAction.get();
        Optional<Form> opForm = action.getFirstForm();
        if (opForm.isPresent()) {
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

          for (String key: headers.keySet()){
            String value = headers.get(key);
            request.addHeader(key, value);
          }
          if (body != null){
            JsonElement element = JsonParser.parseString(body);
            Optional<DataSchema> opSchema = action.getInputSchema();
            if (opSchema.isPresent()){
              request.addHeader("Content-Type", "application/json");
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

          }
          TDHttpResponse response = request.execute();
          com.google.gson.JsonObject responseObject = createResponseObject(response);
          Unifier u =  getTS().getC().getSelectedIntention().peek().getUnif();
          //u.bind(term, new StringTermImpl(response.getPayloadAsString()));
          bindTermToJson(term, responseObject);
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

  public void invokeAction(String tdUrl, String affordanceName, Map<String, String> headers, String body){
  System.out.println("tdUrl: "+tdUrl);
  System.out.println("affordanceName: "+affordanceName);
    try {
      ThingDescription td = TDGraphReader.readFromURL(ThingDescription.TDFormat.RDF_TURTLE, tdUrl);
      System.out.println("td received");
      System.out.println("td: "+ new TDGraphWriter(td).write());
      System.out.println("number of actions: "+td.getActions().size());
      List<ActionAffordance> actions = td.getActions();
      for (ActionAffordance a: actions){
        System.out.println(a.getName());
      }
      System.out.println("affordance name: "+affordanceName);
      Optional<ActionAffordance> opAction = td.getActionByName(affordanceName);
      if (opAction.isPresent()) {
        ActionAffordance action = opAction.get();
        Optional<Form> opForm = action.getFirstForm();
        if (opForm.isPresent()) {
          Form form = opForm.get();
          TDHttpRequest request = new TDHttpRequest(form, TD.invokeAction);
          System.out.println("request defined");
          System.out.println(headers);
          System.out.println("number of headers: "+headers.size());
          for (String key: headers.keySet()){
            System.out.println("key: "+key);
            String value = headers.get(key);
            request.addHeader(key, value);
          }
          if (body != null){
            System.out.println("body: "+body);
            JsonElement element = JsonParser.parseString(body);
            Optional<DataSchema> opSchema = action.getInputSchema();
            if (opSchema.isPresent()){
              request.addHeader("Content-Type", "application/json");
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

          }
          TDHttpResponse response = request.execute();
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

  public void subscribeEvent(String tdUrl, String affordanceName, Map<String, String> headers, String body){
    try {
      ThingDescription td = TDGraphReader.readFromURL(ThingDescription.TDFormat.RDF_TURTLE, tdUrl);
      Optional<EventAffordance> opEvent = td.getEventByName(affordanceName);
      if (opEvent.isPresent()) {
        EventAffordance event = opEvent.get();
        List<Form> forms = event.getForms();
        if (forms.size()>0) {
          Form form = forms.get(0);
          /*TDHttpRequest request = new TDHttpRequest(form, TD.subscribeEvent);
          for (String key: headers.keySet()){
            String value = headers.get(key);
            request.addHeader(key, value);
          }
          if (body != null){
            JsonElement element = JsonParser.parseString(body);
            Optional<DataSchema> opSchema = event.getSubscriptionSchema();
            if (opSchema.isPresent()){
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

          }*/
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

  private List<Object> createArrayPayload(JsonArray jsonArray){
    List<Object> payload = new ArrayList<>();
    for (int i = 0; i<jsonArray.size();i++){
      JsonElement e = jsonArray.get(i);
      payload.add(e);
    }
    return payload;
  }

  private Map<String, Object> createObjectPayload(com.google.gson.JsonObject jsonObject){
    Map<String, Object> payload = new Hashtable<>();
    for (String key: jsonObject.keySet()){
      JsonElement value = jsonObject.get(key);
      payload.put(key, value);
    }
    return payload;
  }

  public void readProperty(String tdUrl, String propertyName, Map<String, String> headers, VarTerm term){
    tdUrl = tdUrl.replace("\"","");
    try {
      ThingDescription td = TDGraphReader.readFromURL(ThingDescription.TDFormat.RDF_TURTLE, tdUrl);
      Optional<PropertyAffordance> opProperty = td.getPropertyByName(propertyName);
      if (opProperty.isPresent()){
        PropertyAffordance property = opProperty.get();
        Optional<Form> opForm = property.getFirstFormForOperationType(TD.readProperty);
        if (opForm.isPresent()){
          Form form = opForm.get();
          TDHttpRequest request = new TDHttpRequest(form, TD.readProperty);
          for (String key: headers.keySet()){
            String value = headers.get(key);
            request.addHeader(key, value);
          }
          TDHttpResponse response = request.execute();
          com.google.gson.JsonObject responseObject = createResponseObject(response);
          bindTermToJson(term, responseObject);
        }
      }

    } catch(Exception e){
      e.printStackTrace();
    }

  }

  public void readProperty(String tdUrl, String propertyName, Map<String, String> headers, ListTerm uriVariableNames, ListTerm uriVariableValues, VarTerm term){
    tdUrl = tdUrl.replace("\"","");
    try {
      ThingDescription td = TDGraphReader.readFromURL(ThingDescription.TDFormat.RDF_TURTLE, tdUrl);
      Optional<PropertyAffordance> opProperty = td.getPropertyByName(propertyName);
      if (opProperty.isPresent()){
        PropertyAffordance property = opProperty.get();
        Optional<Form> opForm = property.getFirstFormForOperationType(TD.readProperty);
        if (opForm.isPresent()){
          Form form = opForm.get();
          TDHttpRequest request = new TDHttpRequest(form, TD.readProperty);
          if (property.getUriVariables().isPresent()) {
            Map<String, Object> values = new Hashtable<>();
            int n = uriVariableNames.size();
            int m = uriVariableValues.size();
            if (n==m){
              for (int i = 0; i <n; i++){
                StringTerm name = (StringTerm) uriVariableNames.get(i);
                StringTerm value = (StringTerm) uriVariableValues.get(i);
                values.put(name.getString(), value.getString());
              }
            }
            request = new TDHttpRequest(form, TD.readProperty, property.getUriVariables().get(), values);
          }
          for (String key: headers.keySet()){
            String value = headers.get(key);
            request.addHeader(key, value);
          }
          TDHttpResponse response = request.execute();
          com.google.gson.JsonObject responseObject = createResponseObject(response);
          bindTermToJson(term, responseObject);
        }
      }

    } catch(Exception e){
      e.printStackTrace();
    }

  }

  public void writeProperty(String tdUrl, String propertyName, Object[] payloadTags, Object[] payload){
    tdUrl = tdUrl.replace("\"","");
    try {
      ThingDescription td = TDGraphReader.readFromURL(ThingDescription.TDFormat.RDF_TURTLE, tdUrl);
      Optional<PropertyAffordance> opProperty = td.getPropertyByName(propertyName);
      if (opProperty.isPresent()){
        PropertyAffordance property = opProperty.get();
        Optional<Form> opForm = property.getFirstFormForOperationType(TD.writeProperty);
        if (opForm.isPresent()){
          Form form = opForm.get();
          TDHttpRequest request = createTDHttpRequest(form, TD.writeProperty, payloadTags, payload);
          request.execute();
        }
      }

    } catch(Exception e){
      e.printStackTrace();
    }

  }

  private TDHttpRequest createTDHttpRequest(Form form, String operationType, Object[] payloadTags, Object[] payload){
    TDHttpRequest request = new TDHttpRequest(form, operationType);
    return request;
  }

  public void setHeader(String key, String value){
    headers.put(key, value);
  }

  public void removeHeader(String key){
    headers.remove(key);
  }




  public com.google.gson.JsonObject sendHttpRequest(String uri, String method, Map<String, String> headers, String body){
    AtomicReference<String> returnValue = new AtomicReference();
    com.google.gson.JsonObject returnObject = new com.google.gson.JsonObject();
    ClassicHttpRequest request = new BasicClassicHttpRequest(method, uri);
    for (String key: headers.keySet()){
      String value = headers.get(key);
      request.addHeader(key, value);
    }

    if (body != null){
      System.out.println("body: "+body);
      if (isJson(body)){
        request.addHeader("Content-Type", "application/json");
      }
      request.setEntity(new StringEntity(body));
    }
    System.out.println("request:"+getRequestRepresentation(request));
    try {
      client.execute(request, response -> {
        returnObject.addProperty("statusCode", response.getCode());
        Iterator<Header> responseHeaders = response.headerIterator();
        com.google.gson.JsonObject rHeaders = new com.google.gson.JsonObject();
        while (responseHeaders.hasNext()){
          Header h = responseHeaders.next();
          rHeaders.addProperty(h.getName(), h.getValue());
        }
        returnObject.add("headers", rHeaders);
        System.out.println("response received: ");
        System.out.println(response.toString());
        HttpEntity entity = response.getEntity();
        //String r = EntityUtils.toString(entity);
        BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
        String line = null;
        String s = "";
        while ((line = reader.readLine())!=null){
          s = s + line;
          System.out.println(line);
        }
        System.out.println(response.getEntity().getContent().toString());
        returnValue.set(s);
        return null;
      });
    } catch(Exception e){
      e.printStackTrace();
    }
    System.out.println("request done");
    returnObject.addProperty("body", returnValue.get());
    return returnObject;

  }

  public com.google.gson.JsonObject sendHttpRequest(String uriTemplate, String method, Map<String, String> headers, ListTerm uriVariableNames, ListTerm uriVariableValues, String body){
    AtomicReference<String> returnValue = new AtomicReference();
    com.google.gson.JsonObject returnObject = new com.google.gson.JsonObject();
    int n = uriVariableNames.size();
    int m = uriVariableValues.size();
    if (n==m) {
      Map<String, DataSchema> uriVariables = new Hashtable<>();
      Map<String, Object> values = new Hashtable<>();
      for (int i = 0; i < n; i++) {
        StringTerm st = (StringTerm) uriVariableNames.get(i);
        String name = st.getString();
        Term t = uriVariableValues.get(i);
        if (t.isString()) {
          StringTerm valueTerm = (StringTerm) t;
          String value = valueTerm.getString();
          uriVariables.put(name, new DataSchema.Builder().build());
          values.put(name, value);
        } else if (t.isNumeric()) {
          NumberTerm nt = (NumberTerm) t;
          try {
            double value = nt.solve();
            uriVariables.put(name, new NumberSchema.Builder().build());
            values.put(name, value);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        String uri = new UriTemplate(uriTemplate).createUri(uriVariables, values);

        ClassicHttpRequest request = new BasicClassicHttpRequest(method, uri);
        for (String key : headers.keySet()) {
          String value = headers.get(key);
          request.addHeader(key, value);
        }

        if (body != null) {
          System.out.println("body: " + body);
          if (isJson(body)) {
            request.addHeader("Content-Type", "application/json");
          }
          request.setEntity(new StringEntity(body));
        }
        try {
          client.execute(request, response -> {
            returnObject.addProperty("statusCode", response.getCode());
            Iterator<Header> responseHeaders = response.headerIterator();
            com.google.gson.JsonObject rHeaders = new com.google.gson.JsonObject();
            while (responseHeaders.hasNext()) {
              Header h = responseHeaders.next();
              rHeaders.addProperty(h.getName(), h.getValue());
            }
            returnObject.add("headers", rHeaders);
            System.out.println("response received: ");
            System.out.println(response.toString());
            HttpEntity entity = response.getEntity();
            //String r = EntityUtils.toString(entity);
            BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
            String line = null;
            String s = "";
            while ((line = reader.readLine()) != null) {
              s = s + line;
              System.out.println(line);
            }
            System.out.println(response.getEntity().getContent().toString());
            returnValue.set(s);
            return null;
          });
        } catch (Exception e) {
          e.printStackTrace();
        }
        System.out.println("request done");
        returnObject.addProperty("body", returnValue.get());
        return returnObject;
      }
    }
    return null;
  }

  public String getRequestRepresentation(ClassicHttpRequest request){
    String str = "url: "+request.getRequestUri()+"\n";
    str = str + "method: "+request.getMethod()+"\n";
    Header[] headers = request.getHeaders();
    str = str + "headers: \n";
    for (int i = 0; i<headers.length; i++){
      Header h = headers[i];
      str = str+"key: "+h.getName()+"; value: "+h.getValue()+"\n";
    }
    try {
      InputStream stream = request.getEntity().getContent();
      String s = new String(stream.readAllBytes());
      str = str + "body: " + s;
    } catch(Exception e){
      e.printStackTrace();
    }
    return str;
  }

  public com.google.gson.JsonObject createResponseObject(TDHttpResponse response){
    com.google.gson.JsonObject responseObject = new com.google.gson.JsonObject();
    responseObject.addProperty("statusCode", response.getStatusCode());
    Map<String,String> responseHeaders = response.getHeaders();
    com.google.gson.JsonObject rHeaders = new com.google.gson.JsonObject();
    for (String key: responseHeaders.keySet()){
      rHeaders.addProperty(key, responseHeaders.get(key));
    }
    responseObject.add("headers", rHeaders);
    Optional<String> payload = response.getPayload();
    if (payload.isPresent()){
      responseObject.addProperty("body", payload.get());
    }
    return responseObject;
  }

  public boolean isInformation(com.google.gson.JsonObject responseObject){
    boolean b = false;
    int code = responseObject.get("statusCode").getAsInt();
    if (code < 200 ){
      b = true;
    }
    return b;
  }

  public boolean isValid(com.google.gson.JsonObject responseObject){
    boolean b = false;
    int code = responseObject.get("statusCode").getAsInt();
    if (code >=200 && code<300 ){
      b = true;
    }
    return b;
  }

  public boolean isRedirection(com.google.gson.JsonObject responseObject){
    boolean b = false;
    int code = responseObject.get("statusCode").getAsInt();
    if (code >=300 && code<400 ){
      b = true;
    }
    return b;
  }

  public boolean isClientError(com.google.gson.JsonObject responseObject){
    boolean b = false;
    int code = responseObject.get("statusCode").getAsInt();
    if (code >=400 && code<500 ){
      b = true;
    }
    return b;
  }

  public boolean isServerError(com.google.gson.JsonObject responseObject){
    boolean b = false;
    int code = responseObject.get("statusCode").getAsInt();
    if (code >500 ){
      b = true;
    }
    return b;
  }



  public boolean isValid(Term jsonId){
    JSONLibrary library = JSONLibrary.getInstance();
    com.google.gson.JsonObject object = library.getJSONElementFromTerm(jsonId).getAsJsonObject();
    return isValid(object);
  }

  public boolean isInformation(Term jsonId){
    JSONLibrary library = JSONLibrary.getInstance();
    com.google.gson.JsonObject object = library.getJSONElementFromTerm(jsonId).getAsJsonObject();
    return isInformation(object);
  }

  public boolean isRedirection(Term jsonId){
    JSONLibrary library = JSONLibrary.getInstance();
    com.google.gson.JsonObject object = library.getJSONElementFromTerm(jsonId).getAsJsonObject();
    return isRedirection(object);
  }

  public boolean isClientError(Term jsonId){
    JSONLibrary library = JSONLibrary.getInstance();
    com.google.gson.JsonObject object = library.getJSONElementFromTerm(jsonId).getAsJsonObject();
    return isClientError(object);
  }

  public boolean isServerError(Term jsonId){
    JSONLibrary library = JSONLibrary.getInstance();
    com.google.gson.JsonObject object = library.getJSONElementFromTerm(jsonId).getAsJsonObject();
    return isServerError(object);
  }

  public String getBody(com.google.gson.JsonObject o){
    return o.get("body").getAsString();
  }

  public String getBody(Term jsonId){
    JSONLibrary jsonLibrary = JSONLibrary.getInstance();
    JsonElement e = jsonLibrary.getJSONElementFromTerm(jsonId);
    if (e.isJsonObject()){
      com.google.gson.JsonObject o = e.getAsJsonObject();
      return getBody(o);
    }
    return null;
  }

  //JSON methods

  public JsonElement getJsonElement(Term jsonId){
    JSONLibrary jsonLibrary = JSONLibrary.getInstance();
    return jsonLibrary.getJSONElementFromTerm(jsonId);


  }

  public JsonElement getAsJsonElement(Term t){
    JsonElement element = null;
    if (t.isString()){
      StringTerm st = (StringTerm) t;
      element = new JsonPrimitive(st.getString());
    } else if (t.isNumeric()){
      NumberTerm nt = (NumberTerm) t;
      try {
        double num = nt.solve();
        element = new JsonPrimitive(num);
        if (num == (int) num){
          System.out.println("is integer");
          int n = (int) num;
          element = new JsonPrimitive(n);
        }
       }
      catch(Exception e){
        e.printStackTrace();
      }
    } else if (t.isList()){
      ListTerm listTerm = (ListTerm) t;
      JsonArray array = new JsonArray();
      for (int i = 0; i<listTerm.size(); i++){
        array.add(getAsJsonElement(listTerm.get(i)));
      }
      element = array;
    } else if (t.isVar()){
      element = getJsonElement(t);
    } else if (t.isLiteral()){
      Literal l = (Literal) t;
      String func = l.getFunctor();
      if (func.equals("true")){
        element = new JsonPrimitive(true);
      } else if (func.equals("false")){
        element = new JsonPrimitive(false);
      } else {
        element = getJsonElement(t);
      }
    }
    return element;
  }

  public boolean hasAttribute(Term jsonId, String attribute){
    boolean b = false;
    JsonElement jsonElement = getJsonElement(jsonId);
    if (jsonElement.isJsonObject()){
      b = jsonElement.getAsJsonObject().keySet().contains(attribute);
    }
    return b;
  }

  public JsonElement getFromJson(Term jsonId, String attribute){
    JsonElement jsonElement = getJsonElement(jsonId);
    if (jsonElement.isJsonObject()){
      com.google.gson.JsonObject jsonObject = jsonElement.getAsJsonObject();
      return jsonObject.get(attribute);
    }
    return null;
  }

  public JsonElement getFromJson(Term jsonId, int index){
    JsonElement jsonElement = getJsonElement(jsonId);
    if (jsonElement.isJsonArray()){
      JsonArray jsonArray = jsonElement.getAsJsonArray();
      return jsonArray.get(index);
    }
    return null;
  }

  public String getStringFromJson(Term jsonId, int index){
    return getFromJson(jsonId, index).getAsString();
  }

  public String getStringFromJson(Term jsonId, String attribute){
    return getFromJson(jsonId, attribute).getAsString();
  }

  public double getNumberFromJson(Term jsonId, int index){
    return getFromJson(jsonId, index).getAsDouble();
  }

  public double getNumberFromJson(Term jsonId, String attribute){
    return getFromJson(jsonId, attribute).getAsDouble();
  }

  public boolean getBooleanFromJson(Term jsonId, int index){
    return getFromJson(jsonId, index).getAsBoolean();
  }

  public boolean getBooleanFromJson(Term jsonId, String attribute){
    return getFromJson(jsonId, attribute).getAsBoolean();
  }

  public Term getAsTerm(Term jsonId){
    JSONLibrary jsonLibrary = JSONLibrary.getInstance();
    return getAsTerm(jsonLibrary.getJSONElementFromTerm(jsonId));

  }

  public Term getAsTerm(JsonElement jsonElement){
    if (jsonElement.isJsonArray()){
      return getAsListTerm(jsonElement);
    } else if (jsonElement.isJsonObject()){
      return getAsMapTerm(jsonElement);
    } else if (jsonElement.isJsonPrimitive()){
      return getAsStringTerm(jsonElement);
    } else {
      return null;
    }
  }

  public NumberTerm getAsNumberTerm(JsonElement jsonElement){
      return new NumberTermImpl(jsonElement.getAsDouble());
  }

  public StringTerm getAsStringTerm(JsonElement jsonElement){
    System.out.println(jsonElement);
    String jsonElementString = jsonElement.toString();
    StringTerm st =  new StringTermImpl(jsonElementString);
    System.out.println("string term: "+st);
    return st;
  }

public StringTerm getAsStringTerm(Term jsonId){
    JSONLibrary library = JSONLibrary.getInstance();
    System.out.println("library retrieved");
    JsonElement json = library.getJSONElementFromTerm(jsonId);
    System.out.println("json element retrieved");
    System.out.println("json element: "+json);
    return getAsStringTerm(json);
}

  public MapTerm getAsMapTerm(JsonElement jsonElement){
    com.google.gson.JsonObject jsonObject = jsonElement.getAsJsonObject();
    MapTerm object = new MapTermImpl();
    for (String key: jsonObject.keySet()){
      object.put(new StringTermImpl(key), getAsTerm(jsonObject.get(key)));
    }
    return object;
  }

  public ListTerm getAsListTerm(JsonElement jsonElement){
    JsonArray jsonArray = jsonElement.getAsJsonArray();
    ListTerm list = new ListTermImpl();
    for (int i = 0; i<jsonArray.size();i++){
      list.add(getAsTerm(jsonArray.get(i)));
    }
    return list;
  }

  public void createJsonObject(Unifier un, ListTerm attributeNames, ListTerm attributeValues, VarTerm jsonId){
    JSONLibrary library = JSONLibrary.getInstance();
    com.google.gson.JsonObject jsonObject = new com.google.gson.JsonObject();
    int n1 = attributeNames.size();
    int n2 = attributeValues.size();
    if (n1==n2) {
      System.out.println("the sizes are equal");
      for (int i = 0; i < n1; i++) {
        Term attributeName = attributeNames.get(i);
        if (attributeName.isString()) {
          StringTerm attributeNameStringTerm = (StringTerm) attributeName;
          jsonObject.add(attributeNameStringTerm.getString(), getAsJsonElement(attributeValues.get(i)));
        }
      }
      String jsonString = jsonObject.toString();
      System.out.println("json created: "+jsonString);
      try {
        library.new_json(un, jsonString, jsonId);
      } catch (Exception e){
        e.printStackTrace();
      }
    } else {
      System.out.println("the sizes are not equal");
    }
  }



  public void printJSON(Term jsonId){
    JSONLibrary jsonLibrary = JSONLibrary.getInstance();
    jsonLibrary.printJson(jsonId);
  }

  public void bindTermToJson(Term jsonId, JsonElement jsonElement){
    JSONLibrary library = JSONLibrary.getInstance();
    Unifier un = getTS().getC().getSelectedIntention().peek().getUnif();
    try {
      library.new_json(un, jsonElement.toString(), jsonId);
    } catch(Exception e){
      e.printStackTrace();
    }
  }

  public Map<String, Object> convertJsonObjectToMap(com.google.gson.JsonObject obj){
    Map<String, Object> map = new Hashtable<>();
    for (String key: obj.keySet()){
      map.put(key, obj.get(key));
    }
    return map;
  }

  public boolean isJson(String body){
    return true;
  }

  //Others

  public String getCurrentTimeStamp1(){
    Timestamp timestamp = Timestamp.from(Instant.now());
    System.out.println(timestamp);
    return timestamp.toString();
  }

  public String getCurrentTimeStamp2(){
    return Double.toString(Instant.now().getEpochSecond());
  }

  public String getCurrentTimeStamp(){
    return Instant.now().toString();
  }










}



