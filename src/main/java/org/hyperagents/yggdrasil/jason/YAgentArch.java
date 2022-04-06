package org.hyperagents.yggdrasil.jason;

import cartago.AgentCredential;
import cartago.AgentIdCredential;
import cartago.ICartagoCallback;
import cartago.Workspace;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.affordances.PropertyAffordance;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpRequest;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpResponse;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class YAgentArch extends AgArch {

  Vertx vertx;
  int messageId;

  public YAgentArch(Vertx vertx){

    this.vertx = vertx;
  }

  public YAgentArch(){
    System.out.println("creating YAgentArch");
    this.vertx = VertxRegistry.getInstance().getVertx();
    System.out.println("vertx: "+vertx);
    //this.vertx = new VertxFactoryImpl().vertx();
    messageId = 0;
  }


  @Override
  public void act(ActionExec actionExec) {
    System.out.println("act");
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

    } else if (func.equals("invokeAction")) {
      String tdUri = terms.get(0).toString();
      String actionName = terms.get(1).toString();
      Map<String, String> headers = new Hashtable<>();
      headers.put("X-Agent-WebID", this.getAgName());
      String body = null;
      if (terms.size() > 2) {
        body = terms.get(2).toString();
      }
      invokeAction(tdUri, actionName, headers, body);
    } else if (func.equals("sendHttpRequest")) {
      String url = terms.get(0).toString();
      System.out.println("url: " + url);
      String method = terms.get(1).toString();
      System.out.println("method: " + method);
      Map<String, String> headers = new Hashtable<>();
      headers.put("X-Agent-WebID", this.getAgName());
      String body = null;
      if (terms.size() > 2) {
        body = terms.get(2).toString();
      }
      sendHttpRequest(url, method, headers, body);
    } else if (func.equals("printJson")) {
      System.out.println("printJson");
      Term jsonId = terms.get(0);
      System.out.println("json id: "+jsonId);
      printJSON(jsonId);
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

        AgentMessageCallback messageCallback = registry.getAgentMessageCallback(this.getAgName());
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
    String response = sendHttpRequest(uri, "PUT", headers, null);
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



  public void invokeAction(String tdUrl, String affordanceName, Map<String, String> headers, String body){
    tdUrl = tdUrl.replace("\"","");
    try {
      ThingDescription td = TDGraphReader.readFromURL(ThingDescription.TDFormat.RDF_TURTLE, tdUrl);
      Optional<ActionAffordance> opAction = td.getActionByName(affordanceName);
      if (opAction.isPresent()) {
        ActionAffordance action = opAction.get();
        Optional<Form> opForm = action.getFirstForm();
        if (opForm.isPresent()) {
        Form form = opForm.get();
        TDHttpRequest request = new TDHttpRequest(form, TD.invokeAction);
        for (String key: headers.keySet()){
          String value = headers.get(key);
          request.addHeader(key, value);
        }
        if (body != null){
          JsonElement element = JsonParser.parseString(body);
          Optional<DataSchema> opSchema = action.getInputSchema();
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


  public String sendHttpRequest(String uri, String method, Map<String, String> headers, String body){
    HttpClient client = HttpClients.createDefault();
    AtomicReference<String> returnValue = new AtomicReference();
    ClassicHttpRequest request = new BasicClassicHttpRequest(method, uri);
    for (String key: headers.keySet()){
      String value = headers.get(key);
      request.addHeader(key, value);
    }
    if (body != null){
      request.setEntity(new StringEntity(body));
    }
    try {
      client.execute(request, response -> {
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
    return returnValue.get();

  }

  //JSON methods

  public void printJSON(Term jsonId){
    JSONLibrary jsonLibrary = JSONLibrary.getInstance();
    jsonLibrary.printJson(jsonId);
  }






}



