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
import ch.unisg.ics.interactions.wot.td.schemas.*;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;
import com.google.gson.*;
import io.vertx.core.Vertx;
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
import org.eclipse.rdf4j.query.algebra.In;
import org.hyperagents.yggdrasil.websub.NotificationSubscriberRegistry;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

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
    Structure action = actionExec.getActionTerm();
    String func = action.getFunctor();
    List<Term> terms = action.getTerms();
    switch (func) {
      case "createWorkspace": { //Inside YAgentArch
        String workspaceName = terms.get(0).toString();
        createWorkspace(workspaceName);
        LOGGER.debug("workspace created");
        break;
      }
      case "createSubWorkspace": { //Inside YAgentArch
        String workspaceName = terms.get(0).toString();
        String subWorkspaceName = terms.get(1).toString();
        createSubWorkspace(workspaceName, subWorkspaceName);
        LOGGER.debug("sub workspace created");
        break;
      }
      case "makeArtifact": { //Inside YAgentArch
        String workspaceName = terms.get(0).toString();
        String artifactName = terms.get(1).toString();
        String artifactInit = terms.get(2).toString();
        makeArtifact(workspaceName, artifactName, artifactInit);
        break;
      }
      case "joinWorkspace": { //Inside YAgentArch
        String workspaceName = terms.get(0).toString();
        joinWorkspace(workspaceName);

        break;
      }
      case "leaveWorkspace": { //Inside YAgentArch
        String workspaceName = terms.get(0).toString();
        leaveWorkspace(workspaceName);
        break;
      }
      case "focus": { //Inside YAgentArch
        String workspaceName = terms.get(0).toString();
        String artifactName = terms.get(1).toString();
        focus(workspaceName, artifactName);

        break;
      }
      case "stopFocus": { //Inside YAgentArch, to develop
        String workspaceName = terms.get(0).toString();
        String artifactName = terms.get(1).toString();
        stopFocus(workspaceName, artifactName);

        break;
      }
      case "setValue": { //To check
        Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
        VarTerm v = (VarTerm) terms.get(0);
        Term t = terms.get(1);
        u.bind(v, t);

        break;
      }
      case "invokeAction": { //Inside wot library
        StringTerm tdUriTerm = (StringTerm) terms.get(0);
        String tdUrl = tdUriTerm.getString();
        StringTerm actionTerm = (StringTerm) terms.get(1);
        String actionName = actionTerm.getString();
        String body = "";
        if (terms.size() > 3) {
          Term t = terms.get(2);
          body = getAsJson(t);
          boolean b = body.startsWith("\"") && body.endsWith("\"");
          while (b){ //TODO: to check
            body = body.substring(1, body.length()-1);
            System.out.println("current body: "+ body);
            b = body.startsWith("\"") && body.endsWith("\"");
          }
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
        Term lastTerm = terms.get(terms.size() - 1);
        if (lastTerm.isVar()) {
          VarTerm v = (VarTerm) lastTerm;
          Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
          u.bind(v, result);
        }
        break;
      }
      case "readProperty": { //Inside wot library
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
        break;
      }
      case "writeProperty": { //Inside wot library
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
        break;
      }
      case "subscribeEvent": {
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
        break;
      }
      case "unsubscribeEvent": {
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
        MapTerm result = unsubscribeEvent(tdUrl, eventName, body, headers, uriVariables);
        Term lastTerm = terms.get(terms.size() - 1);
        if (lastTerm.isVar()) {
          VarTerm v = (VarTerm) lastTerm;
          Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
          u.bind(v, result);
        }
        break;
      }

      case "sendHttpRequest": {//TODO: Create
        StringTerm urlTerm = (StringTerm) terms.get(0);
        String url = urlTerm.getString();
        StringTerm methodTerm = (StringTerm) terms.get(1);
        String method = methodTerm.getString();
        String body = null;
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
        MapTerm result = sendHttpRequest(url, method, body, headers, uriVariables);
        Term lastTerm = terms.get(terms.size() - 1);
        if (lastTerm.isVar()) {
          VarTerm v = (VarTerm) lastTerm;
          Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
          u.bind(v, result);
        }

        break;
      }
      case "createMapTerm": { //TODO: Inside json library
        ListTerm attributes = (ListTerm) terms.get(0);
        ListTerm values = (ListTerm) terms.get(1);
        MapTerm mt = createMapTerm(attributes, values);
        System.out.println("map term created: "+mt);
        Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
        u.bind((VarTerm) terms.get(2), mt);
        break;
      }
      case "createTermFromJson": { //TODO: Inside json library
        Term json = terms.get(0);
        StringTerm jsonStringTerm = (StringTerm) json;
        String jsonString = jsonStringTerm.getString();
        Term t = createTermFromJson(jsonString);
        VarTerm v = (VarTerm) terms.get(1);
        Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
        u.bind(v, t);
        break;
      }
      case "getTermAsJson": { //TODO: Inside json library
        Term json = terms.get(0);
        String jsonString = getAsJson(json);
        StringTerm jsonStringTerm = new StringTermImpl(jsonString);
        VarTerm v = (VarTerm) terms.get(1);
        Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
        u.bind(v, jsonStringTerm);

        break;
      }
      case "getAgHypermediaName": {
        VarTerm v = (VarTerm) terms.get(0);
        String name = this.getAgHypermediaName();
        Unifier u = getTS().getC().getSelectedIntention().peek().getUnif();
        u.bind(v, new StringTermImpl(name));

      }
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
        //notification = transformNotification(notification);
        //System.out.println("new notification: "+ notification);
        Literal belief = Literal.parseLiteral(notification);
        System.out.println("belief: "+ belief);
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
      AgentJasonMessageCallback jasonMessageCallback = registry.getAgentJasonMessageCallback(agentName);
      if (jasonMessageCallback.hasNewMessage()){
        LOGGER.info("agent "+ this.getAgName()+ " has new message");
        String message = messageCallback.retrieveMessage();
        System.out.println("message: "+message);
        Literal messageBelief = new LiteralImpl("message");
        Term id = getNewMessageId();
        System.out.println("id: "+id);
        JsonElement jsonElement = getJsonElementFromString(message);
        if (jsonElement.isJsonObject()){
          com.google.gson.JsonObject messageObject = jsonElement.getAsJsonObject();
          processJasonMessage(messageObject);
        }
        //Term jsonTerm = getJsonFromString(message);
        this.getTS().getAg().addBel(messageBelief);
        messageCallback.noNewMessage();
      }
    } catch(Exception e){
      e.printStackTrace();
    }


    return super.perceive();
  }

  private void processJasonMessage(com.google.gson.JsonObject jsonObject){

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

  private JsonElement getJsonElementFromString(String message) {
    JsonElement jsonElement = null;
    try {
      jsonElement = JsonParser.parseString(message);


    } catch (Exception e){
      e.printStackTrace();
    }
    return jsonElement;
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

  public void createWorkspace(String workspaceName){
    System.out.println("is creating workspace");
    System.out.println("agent: "+this.getAgName());
    Map<String, String> headers = new Hashtable<>();
    headers.put("X-Agent-WebID", this.getAgName());
    headers.put("Slug", workspaceName);
    sendHttpRequest("http://localhost:8080/workspaces/", "POST", null,  headers);
  }


  public void createSubWorkspace(String workspaceName, String subWorkspaceName){
    String uri = "http://localhost:8080/workspaces/" + workspaceName +"/sub";
    Map<String, String> headers = new Hashtable<>();
    headers.put("X-Agent-WebID", this.getAgName());
    JsonObject object = new JsonObject();
    object.put("name", subWorkspaceName);
    String body = object.encode();
    System.out.println("body: "+body);

    sendHttpRequest(uri, "POST", body, headers);

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
    sendHttpRequest(uri, "POST", artifactInit, headers);
  }



  public void joinWorkspace(String workspaceName){
    String uri = "http://localhost:8080/workspaces/"+workspaceName+"/join";
    Map<String, String> headers = new Hashtable<>();
    headers.put("X-Agent-WebID", this.getAgName());
    MapTerm httpResponse = sendHttpRequest(uri, "PUT", null, headers);
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
    sendHttpRequest(uri, "DELETE", null, headers);
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
      sendHttpRequest(focusUri, "PUT", body, headers);
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
      sendHttpRequest(focusUri, "PUT", body, headers);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public MapTerm invokeAction(String tdUrl, String affordanceName, String body, Map<String, String> headers, Map<String, Object> uriVariables){
    try {
      System.out.println("invoke action has body: "+ body);
      ThingDescription td = TDGraphReader.readFromURL(ThingDescription.TDFormat.RDF_TURTLE, tdUrl);
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
          request.addHeader("X-Agent-WebID", this.getAgHypermediaName());

          for (String key: headers.keySet()){
            String value = headers.get(key);
            request.addHeader(key, value);
          }
          if (body != null){
            JsonElement element = JsonParser.parseString(body);
            System.out.println("json element: "+ element);
            Optional<DataSchema> opSchema = action.getInputSchema();
            if (opSchema.isPresent()){
              request.addHeader("Content-Type", "application/json");
              DataSchema schema = opSchema.get();
              if (Objects.equals(schema.getDatatype(), "array") && element.isJsonArray()){
                List<Object> payload = createArrayPayload(element.getAsJsonArray());
                request.setArrayPayload((ArraySchema) schema, payload);
              } else if (Objects.equals(schema.getDatatype(), "object") && element.isJsonObject()){
                Map<String, Object> payload = createObjectPayload(element.getAsJsonObject());
                request.setObjectPayload((ObjectSchema) schema, payload );
              } else if (Objects.equals(schema.getDatatype(), "string")){
                request.setPrimitivePayload(schema, element.getAsString());
              } else if (Objects.equals(schema.getDatatype(), "number")){
                request.setPrimitivePayload(schema, element.getAsDouble());
              } else if (Objects.equals(schema.getDatatype(), "integer")){
                request.setPrimitivePayload(schema, element.getAsLong());
              } else if (Objects.equals(schema.getDatatype(), "boolean")){
                request.setPrimitivePayload(schema, element.getAsBoolean());
              }
            }

          }
          TDHttpResponse response = request.execute();
          return createResponseObject(response);
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
          return createResponseObject(response);
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
          return createResponseObject(response);
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
          return createResponseObject(response);
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
          return createResponseObject(response);
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



  public MapTerm sendHttpRequest(String uri, String method, String body, Map<String, String> headers){
    return sendHttpRequest(uri, method,body, headers, new Hashtable<>());
  }

  public MapTerm sendHttpRequest(String uri, String method, String body, Map<String, String> headers, Map<String, Object> uriVariables){
    AtomicReference<String> returnValue = new AtomicReference<>();
    com.google.gson.JsonObject returnObject = new com.google.gson.JsonObject();
      Map<String, DataSchema> uriVariablesSchemas = new Hashtable<>();
      for (String key: uriVariables.keySet()){
        uriVariablesSchemas.put(key, new StringSchema.Builder().build());
      }
      /*for (int i = 0; i < n; i++) {
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
        }*/
        if (!uriVariables.isEmpty()) {

           uri = new UriTemplate(uri).createUri(uriVariablesSchemas, uriVariables);
        }

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
            System.out.println(response);
            HttpEntity entity = response.getEntity();
            //String r = EntityUtils.toString(entity);
            BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
            String line;
            StringBuilder s = new StringBuilder();
            while ((line = reader.readLine()) != null) {
              s.append(line);
              System.out.println(line);
            }
            System.out.println(response.getEntity().getContent().toString());
            returnValue.set(s.toString());
            return null;
          });
        } catch (Exception e) {
          e.printStackTrace();
        }
        System.out.println("request done");
        returnObject.addProperty("body", returnValue.get());
        return (MapTerm) getAsJsonTerm(returnObject);
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
    payload.ifPresent(s -> responseObject.put(new StringTermImpl("body"), new StringTermImpl(s)));
    return responseObject;
  }

  //JSON methods



  public boolean isJson(String str){ //TODO: check
    boolean b = true;
    try {
      JsonParser.parseString(str);
    } catch (Exception e){
      b = false;
    }
    return b;
  }

  //JSON
/*
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
  }*/

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
      t = null;
    }
    return t;
  }

  public Term createTermFromJson(String jsonString){
    JsonElement jsonElement = JsonParser.parseString(jsonString);
    return getAsJsonTerm(jsonElement);
  }

  public MapTerm createMapTerm(ListTerm attributes, ListTerm values){
    MapTerm mt = new MapTermImpl();
    int n1 = attributes.size();
    int n2 = values.size();
    if (n1 == n2){
      for (int i=0; i<n1;i++){
        Term a = attributes.get(i);
        Term v = values.get(i);
        mt.put(a, v);
      }

    }
    return mt;
  }


  /*public String getAsJson(Term t){
    StringBuilder s = new StringBuilder();
    if (t.isMap()){
      MapTerm mt = (MapTerm) t;
      s = new StringBuilder("{");
      for (Term key: mt.keys()){
        String keyString = key.toString();
        String valueString = getAsJson(mt.get(key));
        s.append(keyString).append(":").append(valueString).append(";");
      }
      s = new StringBuilder(s.substring(0, s.length() - 1));
      s.append("}");

    } else if (t.isList()){
      s = new StringBuilder("[");
      ListTerm lt = (ListTerm) t;
      for (Term term: lt){
        s.append(getAsJson(term)).append(",");
      }
      s = new StringBuilder(s.substring(0, s.length() - 1));
      s.append("]");
    } else if (t.isString()){
      s = new StringBuilder(t.toString());
    } else if (t.isNumeric()){
      NumberTerm nt = (NumberTerm) t;
      try {
        double d = nt.solve();
        long r = Math.round(d);
        if (d == (double)r) {
          s = new StringBuilder(String.valueOf(r));
        } else {
          s = new StringBuilder(String.valueOf(d));
        }
      } catch (Exception e){
        System.err.println("The number is not valid");
      }
    } else if (t.isLiteral()){
      s = new StringBuilder(t.toString());
      System.out.println("literal is : "+ s);
    }
    return s.toString();
  }*/

  public String getAsJson(Term t){
    StringBuilder s = new StringBuilder();
    if (t.isMap()){
      MapTerm mt = (MapTerm) t;
      s = new StringBuilder("{");
      for (Term key: mt.keys()){
        String keyString = key.toString();
        String valueString = getAsJson(mt.get(key));
        s.append(keyString).append(":").append(valueString).append(",");
      }
      s = new StringBuilder(s.substring(0, s.length() - 1));
      s.append("}");

    } else if (t.isList()){
      s = new StringBuilder("[");
      ListTerm lt = (ListTerm) t;
      for (Term term: lt){
        s.append(getAsJson(term)).append(",");
      }
      s = new StringBuilder(s.substring(0, s.length() - 1));
      s.append("]");
    } else if (t.isString()){
      s = new StringBuilder(t.toString());
    } else if (t.isNumeric()){
      NumberTerm nt = (NumberTerm) t;
      try {
        double d = nt.solve();
        long r = Math.round(d);
        if (d == (double)r) {
          s = new StringBuilder(String.valueOf(r));
        } else {
          s = new StringBuilder(String.valueOf(d));
        }
      } catch (Exception e){
        System.err.println("The number is not valid");
      }
    } else if (t.isLiteral()){
      s = new StringBuilder(t.toString());
      System.out.println("literal is : "+ s);
    }
    return s.toString();
  }

  public boolean containsFromIndex(String notification, int index, String pattern){
    boolean b = false;
    String newNotification = notification.substring(index);
    b = newNotification.contains(pattern);
    return b;
  }

  public int getIndexFrom(String notification, int index, String pattern){
    int newIndex;
    String newNotification = notification.substring(index);
    newIndex = newNotification.indexOf(pattern);
    return index + newIndex;
  }

  public String getUriFromIndex(String notification, int index){
    int endIndex = index;
    boolean b = true;
    int i = index;
    while (b && i<notification.length()){
      if (notification.charAt(i) == ',' || notification.charAt(i)==')' ||notification.charAt(i)== ' '){
        b = false;
        endIndex = i;
      } else {
        i++;
      }
    }
    return notification.substring(index, endIndex);
  }

  public Set<String> getUrisFromNotification(String notification){
    Set<String> uris = new HashSet<>();
    int index = 0;
    boolean b = containsFromIndex(notification, index, "http://");
    while (b){
      index = getIndexFrom(notification, index, "http://");
      String uri = getUriFromIndex(notification, index);
      uris.add(uri);
      index = index + uri.length();
      b = containsFromIndex(notification, index, "http://");

    }
    return uris;
  }

  public Set<Integer> allIndexes(String notification, String uri){
    int n = uri.length();
    Set<Integer> allIndexes = new HashSet<>();
    for (int i=0; i<notification.length();i++ ){
      if (notification.substring(i, i+n).equals(uri)){
        allIndexes.add(i);
      }
    }
    return allIndexes;
  }

  public String replace(String notification, String uri, int index){
    String newNotification = notification.substring(0, index);
    newNotification = newNotification + "\"" + uri + "\"" + notification.substring(index + uri.length());
    return newNotification;
  }

  public String replace(String notification, String uri){
    String newNotifaction = notification;
    String returnNotification = notification;
    boolean b = true;
    while (b){
      int index = newNotifaction.lastIndexOf(uri);
      if (index<0){
        b = false;
      } else if (index > 0 && newNotifaction.charAt(index-1)== '\"') {
        newNotifaction = notification.substring(0, index-1);
      } else {
        newNotifaction = notification.substring(0, index-1);
        returnNotification = replace(returnNotification, uri, index);

      }
    }
    return returnNotification;
  }


  public String transformNotification(String notification){
    String newNotification = notification;
    Set<String> uris = getUrisFromNotification(notification);
    Iterator<String> uriIterator= uris.stream().sorted((o1, o2) -> {
      int n1 = o1.length();
      int n2 = o2.length();
      int r = 0;
      if (n1>n2){
        r = -1;
      }
      if (n1<n2){
        r = 1;
      }
      return r;
    }).distinct().iterator();
    for (Iterator<String> it = uriIterator; it.hasNext(); ) {
      String uri = it.next();
      newNotification = replace(newNotification, uri);
    }
    return newNotification;
  }



}



