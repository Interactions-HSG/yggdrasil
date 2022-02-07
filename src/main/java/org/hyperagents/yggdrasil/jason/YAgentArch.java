package org.hyperagents.yggdrasil.jason;

import cartago.AgentCredential;
import cartago.AgentIdCredential;
import cartago.ICartagoCallback;
import cartago.Workspace;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpRequest;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpResponse;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import jason.architecture.AgArch;
import jason.asSemantics.ActionExec;
import jason.asSemantics.Intention;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.hyperagents.yggdrasil.cartago.CartagoVerticle;
import org.hyperagents.yggdrasil.cartago.NotificationCallback;
import org.hyperagents.yggdrasil.cartago.WorkspaceRegistry;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;
import org.hyperagents.yggdrasil.store.RdfStore;
import org.hyperagents.yggdrasil.store.RdfStoreVerticle;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

public class YAgentArch extends AgArch {

  Vertx vertx;

  public YAgentArch(Vertx vertx){
    this.vertx = vertx;
  }

  public YAgentArch(){
    System.out.println("creating YAgentArch");
    this.vertx = VertxRegistry.getInstance().getVertx();
    System.out.println("vertx: "+vertx);
    //this.vertx = new VertxFactoryImpl().vertx();
  }


  @Override
  public void act(ActionExec actionExec){
    System.out.println("act");
    String agentName = getAgName();
    System.out.println("agent name: "+agentName);
    Intention currentIntention = getTS().getC().getSelectedIntention();

    Structure action = actionExec.getActionTerm();

    try {
      boolean failed = false;
      ListTerm lt = action.getAnnots();
      if (lt != null){
        Iterator<Term> it = lt.iterator();
        while (it.hasNext()){
          Term annot = it.next();
        }
      }
      String func = action.getFunctor();
      List<Term> terms = action.getTerms();
      if (func.equals("createWorkspace")){
        String workspaceName = terms.get(0).toString();
        createWorkspace(workspaceName);
        System.out.println("workspace created");
      } else if (func.equals("createSubWorkspace")){
        String workspaceName = terms.get(0).toString();
        String subWorkspaceName = terms.get(1).toString();
        createSubWorkspace(workspaceName, subWorkspaceName);
        System.out.println("sub workspace created");
      } else if (func.equals("makeArtifact")){
        String workspaceName = terms.get(0).toString();
        String artifactName = terms.get(1).toString();
        String artifactInit = terms.get(2).toString();
        makeArtifact(workspaceName, artifactName, artifactInit);
      }
      else if (func.equals("joinWorkspace")){
        String workspaceName = terms.get(0).toString();
        joinWorkspace(workspaceName);

      } else if (func.equals("leaveWorkspace")){
        String workspaceName = terms.get(0).toString();
        leaveWorkspace(workspaceName);
      } else if (func.equals("focus")){

      } else if (func.equals("stopFocus")){

      }
      else if (func.equals("sendHttpRequest")){
        String url = terms.get(0).toString();
        System.out.println("url: "+url);
        String method = terms.get(1).toString();
        System.out.println("method: "+method);
        Map<String, String> headers = new Hashtable<>();
        headers.put("X-Agent-WebID", this.getAgName());
        String body = null;
        if (terms.size()>2){
          body = terms.get(2).toString();
        }
        sendHttpRequest(url, method, headers, body);
      }

    } catch(Exception e){
      e.printStackTrace();
    }
  }


  @Override
  public Collection<Literal> perceive(){
    return super.perceive();
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
      DeliveryOptions options = new DeliveryOptions()
        .addHeader(HttpEntityHandler.REQUEST_METHOD, CartagoVerticle.CREATE_WORKSPACE)
        .addHeader(CartagoVerticle.AGENT_ID, this.getAgName())
        .addHeader(CartagoVerticle.WORKSPACE_NAME, workspaceName);
      String uri = "http://localhost:8080/workspaces/";
      //vertx.eventBus().request(CartagoVerticle.BUS_ADDRESS, "", options, result -> storeEntity(uri, workspaceName, result.result().body().toString(), promise));
    //vertx.eventBus().request(CartagoVerticle.BUS_ADDRESS, "", options, result -> System.out.println("result"));
    Promise<String> environmentPromise = Promise.promise();
    EnvironmentHandler environmentHandler = new EnvironmentHandler(vertx);
    environmentHandler.createWorkspace(this.getAgName(), workspaceName, environmentPromise);
    System.out.println("workspace created");

    environmentPromise.future().compose(result -> Future.future(promise -> storeEntity0(uri, workspaceName, result, promise)));
      System.out.println("end create workspace");

  }

  public EventBus createWorkspace2(String workspaceName){
    DeliveryOptions options = new DeliveryOptions()
      .addHeader(HttpEntityHandler.REQUEST_METHOD, CartagoVerticle.CREATE_WORKSPACE)
      .addHeader(CartagoVerticle.AGENT_ID, this.getAgName())
      .addHeader(CartagoVerticle.WORKSPACE_NAME, workspaceName);
    String uri = "http://localhost:8080/workspaces/";
    Promise<Object> promise = Promise.promise();
    return vertx.eventBus().request(CartagoVerticle.BUS_ADDRESS, "", options, result -> storeEntity0(uri, workspaceName, result.result().body().toString(), promise));

  }

  public void createWorkspace4(String workspaceName){
    String uri = "http://localhost:8080/workspaces/";
    String agentName = this.getAgName();
    boolean b = false;
    System.out.println("before thread");
    Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        DeliveryOptions options = new DeliveryOptions()
          .addHeader(HttpEntityHandler.REQUEST_METHOD, CartagoVerticle.CREATE_WORKSPACE)
          .addHeader(CartagoVerticle.AGENT_ID, agentName)
          .addHeader(CartagoVerticle.WORKSPACE_NAME, workspaceName);
        vertx.eventBus().request(CartagoVerticle.BUS_ADDRESS, "", options, reply -> {
          if (reply.succeeded()){
            DeliveryOptions rdfOptions = new DeliveryOptions()
              .addHeader(HttpEntityHandler.REQUEST_METHOD, RdfStore.CREATE_ENTITY)
              .addHeader(HttpEntityHandler.REQUEST_URI, uri)
              .addHeader(HttpEntityHandler.ENTITY_URI_HINT, workspaceName);
            vertx.eventBus().request(RdfStore.BUS_ADDRESS, reply.result().body().toString(), rdfOptions, result -> {
              if (result.succeeded()) {
                System.out.println("In thread: the entity representation was stored");
              } else {
                System.out.println("In thread: Could not store the entity representation.");
              }
            });
          }
        });
      }
    });
    System.out.println("after thread");
    ControllableThread ct = new ControllableThread(t, 100);
    System.out.println("controllable thread created");
    ct.start();
    System.out.println("controllable thread launched");
    System.out.println("end method create workspace");

  }

  public void createWorkspace5(String workspaceName){
    String uri = "http://localhost:8080/workspaces/";
    boolean b = false;
    DeliveryOptions options = new DeliveryOptions()
      .addHeader(HttpEntityHandler.REQUEST_METHOD, CartagoVerticle.CREATE_WORKSPACE)
      .addHeader(CartagoVerticle.AGENT_ID, this.getAgName())
      .addHeader(CartagoVerticle.WORKSPACE_NAME, workspaceName);
    Promise<String> result = Promise.promise();
    sendEnvironmentRequest("", options, result);
    System.out.println("environment request done");
    DeliveryOptions rdfOptions = new DeliveryOptions()
      .addHeader(HttpEntityHandler.REQUEST_METHOD, RdfStore.CREATE_ENTITY)
      .addHeader(HttpEntityHandler.REQUEST_URI, uri)
      .addHeader(HttpEntityHandler.ENTITY_URI_HINT, workspaceName);
    result.future().compose(r -> {
      return Future.future(promise -> storeEntity(rdfOptions, r, promise));
    });
    System.out.println("method createWorkspace5 completed");

  }

  public void createWorkspace6(String workspaceName){
    Map<String, String> headers = new Hashtable<>();
    headers.put("X-Agent-WebID", this.getAgName());
    headers.put("Slug", workspaceName);
    sendHttpRequest("http://localhost:8080/workspaces/", "POST", headers, null);
  }


  public void createWorkspace(String workspaceName){
    System.out.println("is creating workspace");
    System.out.println("agent: "+this.getAgName());
    createWorkspace6(workspaceName);
  }

  public void createSubWorkspace1(String workspaceName, String subWorkspaceName){
    DeliveryOptions options = new DeliveryOptions()
      .addHeader(HttpEntityHandler.REQUEST_METHOD, CartagoVerticle.CREATE_WORKSPACE)
      .addHeader(CartagoVerticle.AGENT_ID, this.getAgName())
      .addHeader(CartagoVerticle.WORKSPACE_NAME, workspaceName)
      .addHeader(CartagoVerticle.SUB_WORKSPACE_NAME, subWorkspaceName);
    String uri = "http://localhost:8080/workspaces/";
    Promise<Object> promise = Promise.promise();
    vertx.eventBus().request(CartagoVerticle.BUS_ADDRESS, "", options, result -> storeEntity0(uri, subWorkspaceName, result.result().body().toString(), promise));

  }

  public void createSubWorkspace2(String workspaceName, String subWorkspaceName){
    String uri = "http://localhost:8080/workspaces/";
    boolean b = false;
    DeliveryOptions options = new DeliveryOptions()
      .addHeader(HttpEntityHandler.REQUEST_METHOD, CartagoVerticle.CREATE_SUB_WORKSPACE)
      .addHeader(CartagoVerticle.AGENT_ID, this.getAgName())
      .addHeader(CartagoVerticle.WORKSPACE_NAME, workspaceName)
      .addHeader(CartagoVerticle.SUB_WORKSPACE_NAME, subWorkspaceName);
    Promise<String> result = Promise.promise();
    sendEnvironmentRequest("", options, result);
    System.out.println("environment request done");
    DeliveryOptions rdfOptions = new DeliveryOptions()
      .addHeader(HttpEntityHandler.REQUEST_METHOD, RdfStore.CREATE_ENTITY)
      .addHeader(HttpEntityHandler.REQUEST_URI, uri)
      .addHeader(HttpEntityHandler.ENTITY_URI_HINT, workspaceName);
    result.future().compose(r -> {
      return Future.future(promise -> storeEntity(rdfOptions, r, promise));
    });
    System.out.println("method createSubWorkspace2 completed");

  }

  public void createSubWorkspace3(String workspaceName, String subWorkspaceName){
    String uri = "http://localhost:8080/workspaces/" + workspaceName +"/sub";
    Map<String, String> headers = new Hashtable<>();
    headers.put("X-Agent-WebID", this.getAgName());
    JsonObject object = new JsonObject();
    object.put("name", subWorkspaceName);
    String body = object.encode();
    System.out.println("body: "+body);

    sendHttpRequest(uri, "POST", headers, body);

  }

  public void createSubWorkspace(String workspaceName, String subWorkspaceName){
    createSubWorkspace3(workspaceName, subWorkspaceName);
  }

  public void makeArtifact1(String workspaceName, String artifactName, String artifactInit){
    DeliveryOptions options = new DeliveryOptions()
      .addHeader(HttpEntityHandler.REQUEST_METHOD, CartagoVerticle.CREATE_WORKSPACE)
      .addHeader(CartagoVerticle.AGENT_ID, this.getAgName())
      .addHeader(CartagoVerticle.WORKSPACE_NAME, workspaceName)
      .addHeader(CartagoVerticle.ARTIFACT_NAME, artifactName);
    vertx.eventBus().request(CartagoVerticle.BUS_ADDRESS, artifactInit, options, result -> System.out.println(result));

  }

  public void makeArtifact2(String workspaceName, String artifactName, String artifactInit){
    String uri = "http://localhost:8080/workspaces/"+workspaceName+"/artifacts/"+artifactName;
    Map<String, String> headers = new Hashtable<>();
    headers.put("X-Agent-WebID", this.getAgName());
    sendHttpRequest(uri, "POST", headers, artifactInit);
  }

  public void makeArtifact(String workspaceName, String artifactName, String artifactInit){
    makeArtifact2(workspaceName, artifactName, artifactInit);
  }

  public void joinWorkspace1(String workspaceName){
    Workspace workspace = WorkspaceRegistry.getInstance().getWorkspace(workspaceName);
    AgentCredential agentCredential = new AgentIdCredential(getAgName());
    ICartagoCallback callback = new NotificationCallback(vertx);
    try {
      workspace.joinWorkspace(agentCredential, callback);
    } catch(Exception e){
      e.printStackTrace();
    }
  }

  public void joinWorkspace2(String workspaceName){
    DeliveryOptions options = new DeliveryOptions()
      .addHeader(HttpEntityHandler.REQUEST_METHOD, CartagoVerticle.JOIN_WORKSPACE)
      .addHeader(CartagoVerticle.AGENT_ID, this.getAgName())
      .addHeader(CartagoVerticle.WORKSPACE_NAME, workspaceName);
    vertx.eventBus().request(CartagoVerticle.BUS_ADDRESS, "", options, result -> System.out.println(result));
  }

  public void joinWorkspace3(String workspaceName){
    String uri = "http://localhost:8080/workspaces/"+workspaceName+"/join";
    Map<String, String> headers = new Hashtable<>();
    headers.put("X-Agent-WebID", this.getAgName());
    sendHttpRequest(uri, "PUT", headers, null);
  }

  public void joinWorkspace(String workspaceName){
    joinWorkspace3(workspaceName);
  }

  public void leaveWorkspace1(String workspaceName){
    DeliveryOptions options = new DeliveryOptions()
      .addHeader(HttpEntityHandler.REQUEST_METHOD, CartagoVerticle.LEAVE_WORKSPACE)
      .addHeader(CartagoVerticle.AGENT_ID, this.getAgName())
      .addHeader(CartagoVerticle.WORKSPACE_NAME, workspaceName);
    vertx.eventBus().request(CartagoVerticle.BUS_ADDRESS, "", options, result -> System.out.println(result));

  }

  public void leaveWorkspace2(String workspaceName){
    String uri = "http://localhost:8080/workspaces/"+workspaceName+"/leave";
    Map<String, String> headers = new Hashtable<>();
    headers.put("X-Agent-WebID", this.getAgName());
    sendHttpRequest(uri, "DELETE", headers, null);
  }

  public void leaveWorkspace(String workspaceName){
    leaveWorkspace2(workspaceName);
  }

  public void sendHttpRequest1(String url, String method, Map<String, String> headers, String body){
    BasicClassicHttpRequest request = new BasicClassicHttpRequest(method, url);
    for (String name: headers.keySet()){
      String value = headers.get(name);
      request.addHeader(name, value);
    }
    if (body != null){
      request.setEntity(new StringEntity(body));
    }
    HttpClient client = HttpClients.createDefault();
    System.out.println("perform "+request.getMethod()+" on url: "+request.getRequestUri()+" with path: "+request.getPath());
    try {
      ClassicHttpResponse response = (ClassicHttpResponse) client.execute(request);
      HttpEntity entity = response.getEntity();
      System.out.println("response:");
      System.out.println(entity.getContent());
    } catch(Exception e){
      e.printStackTrace();
    }
  }

  public void invokeAction(String tdUrl, String affordanceName, Map<String, String> headers){
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
        TDHttpResponse response = request.execute();
      }
      }
    } catch(Exception e){
      e.printStackTrace();
    }
  }

  public void sendHttpRequest(String url, String method, Map<String, String> headers, String body){
    HttpClient client = HttpClients.createDefault();
    //HttpHost host = new HttpHost(url);
    ClassicHttpRequest request = new BasicClassicHttpRequest(method, url);
    for (String name: headers.keySet()){
      String value = headers.get(name);
      request.addHeader(name, value);
    }
    if (body != null){
      request.setEntity(new StringEntity(body));
    }
    System.out.println("perform "+request.getMethod()+" on url: "+request.getRequestUri()+" with path: "+request.getPath());
    try {
      ClassicHttpResponse response = (ClassicHttpResponse) client.execute(request);
      HttpEntity entity = response.getEntity();
      System.out.println("response:");
      System.out.println(entity.getContent());
    } catch(Exception e){
      e.printStackTrace();
    }
  }



  public void sendHttpRequest2(String url, String method, Map<String, String> headers, String body){
    HttpClientOptions options = new HttpClientOptions();
    io.vertx.core.http.HttpClient client = vertx.createHttpClient();
    if (method.equals("GET")) {
      HttpClientRequest request = client.get(url);
      for (String key: headers.keySet()){
        String value = headers.get(key);
        request.putHeader(key, value);
      }
    }


  }





  private void storeEntity(DeliveryOptions options, String representation, Promise<Object> promise){
    vertx.eventBus().request(RdfStore.BUS_ADDRESS, representation, options, result -> {
      if (result.succeeded()){
        promise.complete("entity has been stored");
      } else {
        promise.fail("entity could not be stored");
      }
    });
    System.out.println("end storeEntity");
  }

  private void storeEntity0(String uri, String entityName, String representation, Promise<Object> promise) {
    System.out.println("uri: "+uri);
    System.out.println("entity name: "+entityName);
    System.out.println("representation: "+representation);
    DeliveryOptions options = new DeliveryOptions()
      .addHeader(HttpEntityHandler.REQUEST_METHOD, RdfStore.CREATE_ENTITY)
      .addHeader(HttpEntityHandler.REQUEST_URI, uri)
      .addHeader(HttpEntityHandler.ENTITY_URI_HINT, entityName);

    vertx.eventBus().request(RdfStore.BUS_ADDRESS, representation, options, result -> {
      if (result.succeeded()) {
        System.out.println("the entity representation was stored");
        promise.complete("the entity representation was stored");
        //context.response().setStatusCode(org.apache.http.HttpStatus.SC_CREATED).end(representation);
        //promise.complete();
      } else {
        System.out.println("Could not store the entity representation.");
        promise.fail("Could not store the entity representation.");
        return;
        //context.response().setStatusCode(org.apache.http.HttpStatus.SC_CREATED).end();
        //promise.fail("Could not store the entity representation.");
      }
    });
    System.out.println("end store entity");
  }

  private void storeEntity1(String uri, String entityName, String representation) {
    System.out.println("uri: "+uri);
    System.out.println("entity name: "+entityName);
    System.out.println("representation: "+representation);
    DeliveryOptions options = new DeliveryOptions()
      .addHeader(HttpEntityHandler.REQUEST_METHOD, RdfStore.CREATE_ENTITY)
      .addHeader(HttpEntityHandler.REQUEST_URI, uri)
      .addHeader(HttpEntityHandler.ENTITY_URI_HINT, entityName);

    vertx.eventBus().request(RdfStore.BUS_ADDRESS, representation, options, reply -> {return;});
    System.out.println("real end store entity");
  }

  private void sendEnvironmentRequest(String message, DeliveryOptions options, Promise<String> result){
    vertx.eventBus().request(CartagoVerticle.BUS_ADDRESS, message, options, reply -> {
      if (reply.succeeded()){
        result.complete(reply.result().body().toString());
      } else {
        result.fail("environment request failed");
      }
    });
  }


}



