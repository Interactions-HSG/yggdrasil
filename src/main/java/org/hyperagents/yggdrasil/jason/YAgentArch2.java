package org.hyperagents.yggdrasil.jason;


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
import com.google.gson.*;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import jason.architecture.AgArch;
import jason.asSemantics.ActionExec;
import jason.asSemantics.Unifier;
import jason.asSyntax.*;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.hyperagents.yggdrasil.cartago.CartagoVerticle;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;
import org.hyperagents.yggdrasil.store.RdfStore;
import org.hyperagents.yggdrasil.websub.NotificationSubscriberRegistry;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class YAgentArch2 extends AgArch {

  Vertx vertx;
  int messageId;
  HttpClient client = HttpClients.createDefault();

  //private JsonManager jsonManager;

  private static final Logger LOGGER = LoggerFactory.getLogger(YAgentArch.class.getName());

  /*public YAgentArch(Vertx vertx){

    this.vertx = vertx;
    this.headers = new Hashtable<>();
    headers.put("X-Agent-WebID", this.getAgName());
  }*/

  public YAgentArch2(){
    LOGGER.info("creating YAgentArch");
    this.vertx = VertxRegistry.getInstance().getVertx();
    //this.vertx = new VertxFactoryImpl().vertx();
    messageId = 0;
    //headers.put("X-Agent-WebID", this.getAgName());
    //this.jsonManager = new JsonManager();
  }



  @Override
  public void act(ActionExec actionExec) {
    LOGGER.info("perform action: " + actionExec.getActionTerm());
    String agentName = getAgName();
    LOGGER.debug("agent name: " + agentName);
    //Intention currentIntention = getTS().getC().getSelectedIntention();
    //Unifier un = currentIntention.peek().getUnif();
    Unifier un = actionExec.getIntention().peek().getUnif();
    Structure action = actionExec.getActionTerm();
    String func = action.getFunctor();
    List<Term> terms = action.getTerms();
    if (func.equals("createWorkspace")) { //Inside YAgentArch
      String workspaceName = terms.get(0).toString();
      createWorkspace(workspaceName);
      LOGGER.debug("workspace created");
    } else if (func.equals("createSubWorkspace")) { //Inside YAgentArch
      String workspaceName = terms.get(0).toString();
      String subWorkspaceName = terms.get(1).toString();
      createSubWorkspace(workspaceName, subWorkspaceName);
      LOGGER.debug("sub workspace created");
    } else if (func.equals("makeArtifact")) { //Inside YAgentArch
      String workspaceName = terms.get(0).toString();
      String artifactName = terms.get(1).toString();
      String artifactInit = terms.get(2).toString();
      makeArtifact(workspaceName, artifactName, artifactInit);
    } else if (func.equals("joinWorkspace")) { //Inside YAgentArch
      String workspaceName = terms.get(0).toString();
      joinWorkspace(workspaceName);

    } else if (func.equals("leaveWorkspace")) { //Inside YAgentArch
      String workspaceName = terms.get(0).toString();
      leaveWorkspace(workspaceName);
    } else if (func.equals("focus")) { //Inside YAgentArch
      String workspaceName = terms.get(0).toString();
      String artifactName = terms.get(1).toString();
      focus(workspaceName, artifactName);

    } else if (func.equals("stopFocus")) { //Inside YAgentArch, to develop
      String workspaceName = terms.get(0).toString();
      String artifactName = terms.get(1).toString();
      stopFocus(workspaceName, artifactName);

    } else if (func.equals("setValue")) { //To check
      Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
      VarTerm v = (VarTerm) terms.get(0);
      Term t = terms.get(1);
      u.bind(v, t);

    } else if (func.equals("invokeAction")) { //Inside wot library
      StringTerm tdUriTerm = (StringTerm) terms.get(0);
      String tdUrl = tdUriTerm.getString();
      StringTerm actionTerm = (StringTerm) terms.get(1);
      String actionName = actionTerm.getString();
      String body = "";
      if (terms.size() > 3) {
        Term t = terms.get(2);
        body = getAsJson(t);
      }
      Map<String, String> headers = new Hashtable<>();
      if (terms.size() > 4) {
        MapTerm headersMap = (MapTerm) terms.get(3);
        for (Term key : headersMap.keys()) {
          headers.put(key.toString(), headersMap.get(key).toString());
        }
      }
      Map<String, Object> uriVariables = new Hashtable<>();
      if (terms.size() > 5) {
        MapTerm uriVariablesMap = (MapTerm) terms.get(4);
        for (Term key : uriVariablesMap.keys()) {
          headers.put(key.toString(), uriVariablesMap.get(key).toString());
        }
      }
      MapTerm result = invokeAction(tdUrl, actionName, body, headers, uriVariables);
      Term lastTerm = terms.get(terms.size() - 1);
      if (lastTerm.isVar()) {
        VarTerm v = (VarTerm) lastTerm;
        Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
        u.bind(v, result);
      }
    } else if (func.equals("readProperty")) { //Inside wot library
      StringTerm tdUriTerm = (StringTerm) terms.get(0);
      String tdUrl = tdUriTerm.getString();
      StringTerm propertyTerm = (StringTerm) terms.get(1);
      String propertyName = propertyTerm.getString();
      Map<String, String> headers = new Hashtable<>();
      Map<String, Object> uriVariables = new Hashtable<>();
      if (terms.size() > 3) {
        MapTerm headersMap = (MapTerm) terms.get(2);
        for (Term key : headersMap.keys()) {
          headers.put(key.toString(), headersMap.get(key).toString());
        }
      }
      if (terms.size() > 4) {
        MapTerm uriVariablesMap = (MapTerm) terms.get(3);
        for (Term key : uriVariablesMap.keys()) {
          headers.put(key.toString(), uriVariablesMap.get(key).toString());
        }
      }
      MapTerm result = readProperty(tdUrl, propertyName, headers, uriVariables);
      Term lastTerm = terms.get(terms.size() - 1);
      if (lastTerm.isVar()) {
        VarTerm v = (VarTerm) lastTerm;
        Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
        u.bind(v, result);
      }
    } else if (func.equals("writeProperty")) { //Inside wot library
      StringTerm tdUriTerm = (StringTerm) terms.get(0);
      String tdUrl = tdUriTerm.getString();
      StringTerm propertyTerm = (StringTerm) terms.get(1);
      String propertyName = propertyTerm.getString();
      String body = "";
      if (terms.size() > 3) {
        Term t = terms.get(2);
        body = getAsJson(t);
      }
      Map<String, String> headers = new Hashtable<>();
      Map<String, Object> uriVariables = new Hashtable<>();
      if (terms.size() > 4) {
        MapTerm headersMap = (MapTerm) terms.get(3);
        for (Term key : headersMap.keys()) {
          headers.put(key.toString(), headersMap.get(key).toString());
        }
      }
      if (terms.size() > 5) {
        MapTerm uriVariablesMap = (MapTerm) terms.get(4);
        for (Term key : uriVariablesMap.keys()) {
          headers.put(key.toString(), uriVariablesMap.get(key).toString());
        }
      }
      MapTerm result = writeProperty(tdUrl, propertyName, body, headers, uriVariables);
      Term lastTerm = terms.get(terms.size() - 1);
      if (lastTerm.isVar()) {
        VarTerm v = (VarTerm) lastTerm;
        Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
        u.bind(v, result);
      }
    } else if (func.equals("subscribeEvent")) {
      StringTerm tdUriTerm = (StringTerm) terms.get(0);
      String tdUrl = tdUriTerm.getString();
      StringTerm eventTerm = (StringTerm) terms.get(1);
      String eventName = eventTerm.getString();
      String body = "";
      if (terms.size() > 3) {
        Term t = terms.get(2);
        body = getAsJson(t);
      }
      Map<String, String> headers = new Hashtable<>();
      if (terms.size() > 4) {
        MapTerm headersMap = (MapTerm) terms.get(3);
        for (Term key : headersMap.keys()) {
          headers.put(key.toString(), headersMap.get(key).toString());
        }
      }
      Map<String, Object> uriVariables = new Hashtable<>();
      if (terms.size() > 5) {
        MapTerm uriVariablesMap = (MapTerm) terms.get(4);
        for (Term key : uriVariablesMap.keys()) {
          headers.put(key.toString(), uriVariablesMap.get(key).toString());
        }
      }
      MapTerm result = subscribeEvent(tdUrl, eventName, body, headers, uriVariables);
      Term lastTerm = terms.get(terms.size() - 1);
      if (lastTerm.isVar()) {
        VarTerm v = (VarTerm) lastTerm;
        Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
        u.bind(v, result);
      }
    } else if (func.equals("sendHttpRequest")) { //TODO: Create

    } else if (func.equals("createTermFromJson")) { //Inside json library
      Term json = terms.get(0);
      //String jsonString = json.toString();
      StringTerm jsonStringTerm = (StringTerm) json;
      String jsonString = jsonStringTerm.getString();
      System.out.println("json string: " + jsonString);
      Term t = createTermFromJson(jsonString);
      VarTerm v = (VarTerm) terms.get(1);
      Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
      u.bind(v, t);
    } else if (func.equals("getTermAsJson")) {
      Term json = terms.get(0);
      String jsonString = getAsJson(json);
      StringTerm jsonStringTerm = new StringTermImpl(jsonString);
      VarTerm v = (VarTerm) terms.get(1);
      Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
      u.bind(v, jsonStringTerm);

    } else if (func.equals("createTermFromJson")) {
      StringTerm jsonStringTerm = (StringTerm) terms.get(0);
      String jsonString = jsonStringTerm.getString();
      Term t = createTermFromJson(jsonString);
      VarTerm v = (VarTerm) terms.get(1);
      un.bind(v, t);
    }

    LOGGER.info("end method act");
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
        LOGGER.info("notification received: " + notification);
        Literal belief = Literal.parseLiteral(notification);
        this.getTS().getAg().addBel(belief);
      }
      String agentName = this.getAgName();
      AgentMessageCallback messageCallback = registry.getAgentMessageCallback(agentName);
      if (messageCallback.hasNewMessage()) {
        LOGGER.info("agent "+ this.getAgName()+ " has new message");
        String message = messageCallback.retrieveMessage();
        System.out.println("message: "+message);
        Literal messageBelief = new LiteralImpl("message");
        Term id = getNewMessageId();
        System.out.println("id: "+id);

        Term jsonTerm = getJsonFromString(message);
        System.out.println("json term: "+ jsonTerm);
        messageBelief.addTerm(id);
        messageBelief.addTerm(jsonTerm);
        System.out.println("message belief: "+ messageBelief);
        this.getTS().getAg().addBel(messageBelief);
        messageCallback.noNewMessage();
      }
    } catch(Exception e){
      e.printStackTrace();
    }


    return super.perceive();
  }

  public String getAgHypermediaName(){
    String agentUri = "";
    try {
      agentUri = AgentRegistry.getInstance().getAgentUri(this.getAgName());
      System.out.println("agent uri: "+ agentUri);
    } catch (Exception e){
      System.err.println("The agent has no hypermedia name");
    }
    return agentUri;
  }
  private Term getJsonFromString(String message) { //TODO: check
    Term t = null;
    try {
      JsonElement jsonElement = JsonParser.parseString(message);
      t =  getAsJsonTerm(jsonElement);

    } catch (Exception e){
      e.printStackTrace();
    }
    return t;
  }

  public Term getAsTerm(JsonElement jsonElement){
    if (jsonElement.isJsonArray()){
      return getAsListTerm(jsonElement);
    } else if (jsonElement.isJsonObject()){
      return getAsMapTerm(jsonElement);
    } else if (jsonElement.isJsonPrimitive()){
      return getAsPrimitiveTerm(jsonElement);
    } else {
      return null;
    }
  }

  public Term getAsPrimitiveTerm(JsonElement jsonElement){
    JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();
    if (jsonPrimitive.isString()){
      return new StringTermImpl(jsonPrimitive.getAsString());
    } else if (jsonPrimitive.isNumber()){
      return new NumberTermImpl(jsonPrimitive.getAsDouble());
    } else if (jsonPrimitive.isBoolean()){
      boolean b = jsonPrimitive.getAsBoolean();
      if (b){
        return Literal.LTrue;
      } else {
        return Literal.LFalse;
      }
    }
    return null; //TODO: check
  }

  public ListTerm getAsListTerm(JsonElement jsonElement){
    JsonArray jsonArray = jsonElement.getAsJsonArray();
    ListTerm list = new ListTermImpl();
    for (int i = 0; i<jsonArray.size();i++){
      list.add(getAsTerm(jsonArray.get(i)));
    }
    return list;
  }

  public MapTerm getAsMapTerm(JsonElement jsonElement){
    com.google.gson.JsonObject jsonObject = jsonElement.getAsJsonObject();
    MapTerm object = new MapTermImpl();
    for (String key: jsonObject.keySet()){
      object.put(new StringTermImpl(key), getAsTerm(jsonObject.get(key)));
    }
    return object;
  }

  private Term getNewMessageId(){
    Term messageTermId = new StringTermImpl("Message"+this.messageId);
    this.messageId ++;
    return messageTermId;
  }







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
    MapTerm httpResponse = sendHttpRequest(uri, "PUT", headers, null);
    String response = ((StringTerm) httpResponse.get(new StringTermImpl("body"))).getString();
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

  public void stopFocus(String workspaceName, String artifactName) {
    try {
      String bodyName = AgentRegistry.getInstance().getBody(this.getAgName(), workspaceName);
      String focusUri = bodyName + "/stopFocus";
      System.out.println("body name: " + bodyName);
      Map<String, String> headers = new Hashtable<>();
      headers.put("X-Agent-WebID", this.getAgName());
      headers.put("Content-Type", "application/json");
      String body = "[\"" + artifactName + "\"]";
      System.out.println("focus body: " + body);
      sendHttpRequest(focusUri, "PUT", headers, body);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public MapTerm invokeAction(String tdUrl, String affordanceName, String body, Map<String, String> headers, Map<String, Object> uriVariables){
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
            System.out.println("form target: "+form.getTarget());
            request = new TDHttpRequest(form, TD.invokeAction, action.getUriVariables().get(), uriVariables);
            System.out.println(request.getTarget());
          }
          request.addHeader("X-Agent-WebID", this.getAgHypermediaName());

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
          MapTerm responseObject = createResponseObject(response);
          return responseObject;
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
            System.out.println(request.getTarget());
          }
          request.addHeader("X-Agent-WebID", this.getAgHypermediaName());

          for (String key: headers.keySet()){
            String value = headers.get(key);
            request.addHeader(key, value);
          }
          TDHttpResponse response = request.execute();
          MapTerm responseObject = createResponseObject(response);
          return responseObject;
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


  public MapTerm writeProperty(String tdUrl, String affordanceName, String body, Map<String, String> headers, Map<String, Object> uriVariables){
    try {
      ThingDescription td = TDGraphReader.readFromURL(ThingDescription.TDFormat.RDF_TURTLE, tdUrl);
      Optional<PropertyAffordance> opProperty = td.getPropertyByName(affordanceName);
      if (opProperty.isPresent()) {
        PropertyAffordance property = opProperty.get();
        List<Form> formList= property.getForms();
        if (formList.size()>0) {
          Form form = formList.get(0);
          TDHttpRequest request = new TDHttpRequest(form, TD.writeProperty);
          if (property.getUriVariables().isPresent()) {
            System.out.println("form target: "+form.getTarget());
            request = new TDHttpRequest(form, TD.writeProperty, property.getUriVariables().get(), uriVariables);
            System.out.println(request.getTarget());
          }
          request.addHeader("X-Agent-WebID", this.getAgHypermediaName());

          for (String key: headers.keySet()){
            String value = headers.get(key);
            request.addHeader(key, value);
          }
          request.setPrimitivePayload(new StringSchema.Builder().build(), body);
          TDHttpResponse response = request.execute();
          MapTerm responseObject = createResponseObject(response);
          return responseObject;
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


  public MapTerm subscribeEvent(String tdUrl, String affordanceName, String body, Map<String, String> headers, Map<String, Object> uriVariables){
    try {
      ThingDescription td = TDGraphReader.readFromURL(ThingDescription.TDFormat.RDF_TURTLE, tdUrl);
      Optional<EventAffordance> opEvent = td.getEventByName(affordanceName);
      if (opEvent.isPresent()) {
        EventAffordance event = opEvent.get();
        List<Form> formList= event.getForms();
        if (formList.size()>0) {
          Form form = formList.get(0);
          TDHttpRequest request = new TDHttpRequest(form, TD.subscribeEvent);
          if (event.getUriVariables().isPresent()) {
            System.out.println("form target: "+form.getTarget());
            request = new TDHttpRequest(form, TD.subscribeEvent, event.getUriVariables().get(), uriVariables);
            System.out.println(request.getTarget());
          }
          request.addHeader("X-Agent-WebID", this.getAgHypermediaName());

          for (String key: headers.keySet()){
            String value = headers.get(key);
            request.addHeader(key, value);
          }
          request.setPrimitivePayload(new StringSchema.Builder().build(), body); //TODO: change
          TDHttpResponse response = request.execute();
          MapTerm responseObject = createResponseObject(response);
          //Unifier u =  getTS().getC().getSelectedIntention().peek().getUnif();
          //u.bind(term, new StringTermImpl(response.getPayloadAsString()));
          return responseObject;
        } else {
          System.out.println("form is not present");
          return null;
        }
      } else {
        System.out.println("event is not present");
        return null;
      }
    } catch(Exception e){
      e.printStackTrace();
    }
    return null;
  }

  public MapTerm unsubscribeEvent(String tdUrl, String affordanceName, String body, Map<String, String> headers, Map<String, Object> uriVariables){
    try {
      ThingDescription td = TDGraphReader.readFromURL(ThingDescription.TDFormat.RDF_TURTLE, tdUrl);
      Optional<EventAffordance> opEvent = td.getEventByName(affordanceName);
      if (opEvent.isPresent()) {
        EventAffordance event = opEvent.get();
        List<Form> formList= event.getForms();
        if (formList.size()>0) {
          Form form = formList.get(0);
          TDHttpRequest request = new TDHttpRequest(form, TD.unsubscribeEvent);
          if (event.getUriVariables().isPresent()) {
            System.out.println("form target: "+form.getTarget());
            request = new TDHttpRequest(form, TD.unsubscribeEvent, event.getUriVariables().get(), uriVariables);
            System.out.println(request.getTarget());
          }
          request.addHeader("X-Agent-WebID", this.getAgHypermediaName());

          for (String key: headers.keySet()){
            String value = headers.get(key);
            request.addHeader(key, value);
          }
          request.setPrimitivePayload(new StringSchema.Builder().build(), body); //TODO: change
          TDHttpResponse response = request.execute();
          MapTerm responseObject = createResponseObject(response);
          //Unifier u =  getTS().getC().getSelectedIntention().peek().getUnif();
          //u.bind(term, new StringTermImpl(response.getPayloadAsString()));
          return responseObject;
        } else {
          System.out.println("form is not present");
          return null;
        }
      } else {
        System.out.println("event is not present");
        return null;
      }
    } catch(Exception e){
      e.printStackTrace();
    }
    return null;
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




  private TDHttpRequest createTDHttpRequest(Form form, String operationType, Object[] payloadTags, Object[] payload){
    TDHttpRequest request = new TDHttpRequest(form, operationType);
    return request;
  }



  public MapTerm sendHttpRequest(String uri, String method, Map<String, String> headers, String body){
    AtomicReference<String> returnValue = new AtomicReference();
    //com.google.gson.JsonObject returnObject = new com.google.gson.JsonObject();
    MapTerm returnObject = new MapTermImpl();
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
        returnObject.put(new StringTermImpl("status"), new NumberTermImpl(response.getCode()));
        Iterator<Header> responseHeaders = response.headerIterator();
        //com.google.gson.JsonObject rHeaders = new com.google.gson.JsonObject();
        MapTerm rHeaders = new MapTermImpl();
        while (responseHeaders.hasNext()){
          Header h = responseHeaders.next();
          rHeaders.put(new StringTermImpl(h.getName()), new StringTermImpl(h.getValue()));
        }
        returnObject.put(new StringTermImpl("headers"), rHeaders);
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
    returnObject.put(new StringTermImpl("body"), new StringTermImpl(returnValue.get()));
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

  public MapTerm createResponseObject(TDHttpResponse response){
    MapTerm responseObject = new MapTermImpl();
    responseObject.put(new StringTermImpl("statusCode"), new NumberTermImpl(response.getStatusCode()));
    Map<String,String> responseHeaders = response.getHeaders();
    MapTerm rHeaders = new MapTermImpl();
    for (String key: responseHeaders.keySet()){
      rHeaders.put(new StringTermImpl(key), new StringTermImpl(responseHeaders.get(key)));
    }
    responseObject.put(new StringTermImpl("headers"), rHeaders);
    Optional<String> payload = response.getPayload();
    if (payload.isPresent()){
      responseObject.put(new StringTermImpl("body"), new StringTermImpl(payload.get()));
    }
    return responseObject;
  }




  public boolean isValid(Term jsonId){
    boolean b = false;
    MapTerm jsonObject = (MapTerm) jsonId;
    NumberTerm n = (NumberTerm) jsonObject.get(new StringTermImpl("statusCode"));
    try {
      double v = n.solve();
      if (v>=200 && v<300){
        b = true;
      }
    } catch (Exception e){
      e.printStackTrace();
    }
    return b;
  }

  public boolean isInformation(Term jsonId){
    boolean b = false;
    MapTerm jsonObject = (MapTerm) jsonId;
    NumberTerm n = (NumberTerm) jsonObject.get(new StringTermImpl("statusCode"));
    try {
      double v = n.solve();
      if (v<200){
        b = true;
      }
    } catch (Exception e){
      e.printStackTrace();
    }
    return b;
  }

  public boolean isRedirection(Term jsonId){
    boolean b = false;
    MapTerm jsonObject = (MapTerm) jsonId;
    NumberTerm n = (NumberTerm) jsonObject.get(new StringTermImpl("statusCode"));
    try {
      double v = n.solve();
      if (v>=300 && v<400){
        b = true;
      }
    } catch (Exception e){
      e.printStackTrace();
    }
    return b;

  }

  public boolean isClientError(Term jsonId){
    boolean b = false;
    MapTerm jsonObject = (MapTerm) jsonId;
    NumberTerm n = (NumberTerm) jsonObject.get(new StringTermImpl("statusCode"));
    try {
      double v = n.solve();
      if (v>=400 && v<500){
        b = true;
      }
    } catch (Exception e){
      e.printStackTrace();
    }
    return b;
  }

  public boolean isServerError(Term jsonId){
  boolean b = false;
  MapTerm jsonObject = (MapTerm) jsonId;
  NumberTerm n = (NumberTerm) jsonObject.get(new StringTermImpl("statusCode"));
  try {
  double v = n.solve();
  if (v>=500){
  b = true;
  }
  } catch (Exception e){
  e.printStackTrace();
  }
  return b;
}

  public String getBody(MapTerm jsonObject){
  return jsonObject.get(new StringTermImpl("body")).toString();
  }



  //JSON methods





  public boolean hasAttribute(Term jsonId, String attribute){
    boolean b = false;
    if (jsonId.isMap()) {
      MapTerm jsonObject = (MapTerm) jsonId;
      if (jsonObject.get(new StringTermImpl("attribute")) != null) {
        b = true;
      }
    }
    return b;
  }

  public Term getFromJson(Term jsonId, String attribute){
    Term t = null;
    if (jsonId.isMap()) {
      MapTerm jsonObject = (MapTerm) jsonId;
      if (jsonObject.get(new StringTermImpl("attribute")) != null) {
        t = jsonObject.get(new StringTermImpl(attribute));
      }
    }
    return t;
  }

  public Term getFromJson(Term jsonId, int index){
    if (jsonId.isList()){
      ListTerm listTerm = (ListTerm) jsonId;
      return listTerm.get(index);
    }
    return null;
  }

  public String getStringFromJson(Term jsonId, int index){
    Term t = getFromJson(jsonId, index);
    if (t.isString()){
      StringTerm st = (StringTerm) t;
      return st.getString();
    }
    return null;
  }

  public String getStringFromJson(Term jsonId, String attribute){
    Term t = getFromJson(jsonId, attribute);
    if (t.isString()){
      StringTerm st = (StringTerm) t;
      return st.getString();
    }
    return null;
  }

  public double getNumberFromJson(Term jsonId, int index){
    Term t = getFromJson(jsonId, index);
    if (t.isNumeric()){
      NumberTerm nt = (NumberTerm) t;
      try {
        return nt.solve();
      } catch (Exception e){
        e.printStackTrace();
      }
    }
    return 0; //TODO: check
  }

  public double getNumberFromJson(Term jsonId, String attribute){
    Term t = getFromJson(jsonId, attribute);
    if (t.isNumeric()){
      NumberTerm nt = (NumberTerm) t;
      try {
        return nt.solve();
      } catch (Exception e){
        e.printStackTrace();
      }
    }
    return 0; //TODO: check
  }











  public Map<String, Object> convertJsonObjectToMap(MapTerm obj){
    Map<String, Object> map = new Hashtable<>();
    for (Term t: obj.keys()) {
      if (t.isString()) {
        StringTerm st = (StringTerm) t;
        map.put(st.getString(), obj.get(t));
      }
    }
    return map;
  }

  public boolean isJson(String str){ //TODO: check
    boolean b = true;
    try {
      JsonParser.parseString(str);
    } catch (Exception e){
      b = false;
    }
    return b;
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

  //JSON

  public MapTerm createJsonObjectTerm(List<String> keys, List<Term> values){
    MapTerm jsonTerm = new MapTermImpl();
    if (keys.size()==values.size()){
      for (int i=0; i<keys.size(); i++){
        jsonTerm.put(new StringTermImpl(keys.get(i)), values.get(i));
      }
    }
    return jsonTerm;
  }

  public ListTerm createJsonArrayTerm(List<Term> values){
    ListTerm listTerm = new ListTermImpl();
    for (Term t: values){
      listTerm.add(t);
    }
    return listTerm;
  }

  public Term getElementFromJson(MapTerm jsonTerm, StringTerm attribute){
    return jsonTerm.get(attribute);
  }

  public StringTerm getStringTermFromJson(MapTerm jsonTerm, StringTerm attribute){
    Term t =  jsonTerm.get(attribute);
    if (t.isString()){
      return (StringTerm) t;
    } else {
      return new StringTermImpl();
    }
  }

  public NumberTerm getNumberTermFromJson(MapTerm jsonTerm, StringTerm attribute){
    Term t =  jsonTerm.get(attribute);
    if (t.isNumeric()){
      return (NumberTerm) t;
    } else {
      return new NumberTermImpl();
    }
  }



  public Term getAsJsonTerm(JsonElement jsonElement){
    Term t = new MapTermImpl();
    if (jsonElement.isJsonPrimitive()){
      JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();
      if (jsonPrimitive.isNumber()){
        t = new NumberTermImpl(jsonPrimitive.getAsDouble());
      } else if (jsonPrimitive.isString()){
        t = new StringTermImpl(jsonPrimitive.getAsString());
      } else if (jsonPrimitive.isBoolean()){
        System.out.println("has boolean");
        boolean b = jsonPrimitive.getAsBoolean();
        if (b){
          t = Literal.LTrue;
          System.out.println("boolean: "+t);
        } else {
          t = Literal.LFalse;
          System.out.println("boolean: "+t);
        }
      }
    } else if (jsonElement.isJsonArray()){
      JsonArray jsonArray = jsonElement.getAsJsonArray();
      ListTerm l =  new ListTermImpl();
      for (int i = 0; i<jsonArray.size(); i++){
        l.add(getAsJsonTerm(jsonArray.get(i)));
      }
      t = l;
    } else if (jsonElement.isJsonObject()){
      com.google.gson.JsonObject jsonObject = jsonElement.getAsJsonObject();
      MapTerm mapTerm = new MapTermImpl();
      for (String key : jsonObject.keySet()){
        mapTerm.put(new StringTermImpl(key), getAsJsonTerm(jsonObject.get(key))); //TODO: check
      }
      t= mapTerm;
    } else if (jsonElement.isJsonNull()){
      JsonNull jsonNull = jsonElement.getAsJsonNull();
    }
    return t;
  }

  public Term createTermFromJson(String jsonString){
    JsonElement jsonElement = JsonParser.parseString(jsonString);
    return getAsJsonTerm(jsonElement);
  }

  public String getAsJson1(Term t){
    String s = t.getAsJSON(""); //TODO: does not work for boolean
    System.out.println("json string: "+s);
    JsonElement jsonElement = JsonParser.parseString(s); //TODO: make it work
    s = jsonElement.toString();
    System.out.println("json element: "+ s);
    return s;
  }

  public String getAsJson(Term t){
    String s = "";
    if (t.isMap()){
      MapTerm mt = (MapTerm) t;
      s = "{";
      for (Term key: mt.keys()){
        String keyString = key.toString();
        String valueString = getAsJson(mt.get(key));
        s = s + keyString + ":" + valueString+ ";";
      }
      s = s.substring(0, s.length()-1);
      s = s + "}";

    } else if (t.isList()){
      s = "[";
      ListTerm lt = (ListTerm) t;
      for (Term term: lt){
        s = s + getAsJson(term) + ",";
      }
      s = s.substring(0 , s.length()-1);
      s = s + "]";
    } else if (t.isString()){
      StringTerm st = (StringTerm) t;
      s = t.toString();
    } else if (t.isNumeric()){
      NumberTerm nt = (NumberTerm) t;
      try {
        double d = nt.solve();
        long r = Math.round(d);
        if (d == (double)r) {
          s = String.valueOf(r);
        } else {
          s = String.valueOf(d);
        }
      } catch (Exception e){
        System.err.println("The number is not valid");
      }
    } else if (t.isLiteral()){
      s = t.toString();
      System.out.println("literal is : "+ s);
    }
    return s;
  }











}



