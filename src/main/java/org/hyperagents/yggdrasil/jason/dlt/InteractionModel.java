package org.hyperagents.yggdrasil.jason.dlt;


import ch.unisg.ics.interactions.wot.td.clients.TDHttpRequest;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpResponse;
import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import io.vertx.core.json.Json;
import jason.asSyntax.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public class InteractionModel {
    private MapTerm modelTerm;


    private Optional<String> tdUrl;

    private Optional<String> affordanceName;

    private Optional<String> agentUri;

    private static ValueFactory rdf = SimpleValueFactory.getInstance();

    public InteractionModel(MapTerm modelTerm){
      this.modelTerm = modelTerm;
      this.tdUrl = Optional.empty();
      this.affordanceName = Optional.empty();
      this.agentUri = Optional.empty();
    }

    public void setTDInfo(String tdUrl, String affordanceName){
      this.tdUrl = Optional.of(tdUrl);
      this.affordanceName = Optional.of(affordanceName);
    }

  public void setAgentInfo(String agentUri){
    this.agentUri = Optional.of(agentUri);
  }

  public JsonElement getAsJson(Term t){
      JsonElement e = null;
    if (t.isMap()){
      MapTerm mt = (MapTerm) t;
      com.google.gson.JsonObject o = new com.google.gson.JsonObject();
      for (Term key: mt.keys()){
        o.add(cleanString(key.toString()), getAsJson(mt.get(key)) );
      }
      e = o;
    } else if (t.isList()){
      JsonArray array = new JsonArray();
      ListTerm lt = (ListTerm) t;
      for (Term term: lt){
        array.add(getAsJson(term));
      }
      e = array;
    } else if (t.isString()){
      e = new JsonPrimitive(cleanString(t.toString()));
    } else if (t.isNumeric()){
      NumberTerm nt = (NumberTerm) t;
      try {
        double d = nt.solve();
        long r = Math.round(d);
        if (d == (double)r) {
          e = new JsonPrimitive(r);
        } else {
          e = new JsonPrimitive(d);
        }
      } catch (Exception ex){
        System.err.println("The number is not valid");
      }
    } else if (t.isLiteral()){
      e = new JsonPrimitive(cleanString(t.toString()));
    }
    return e;
  }

    public JsonObject getJsonObjectFromMapTerm(MapTerm modelTerm){
      JsonElement e = getAsJson(modelTerm);
      System.out.println("element e: "+ e);
      return e.getAsJsonObject();

    }

    public Map<String, String> getHeaders(com.google.gson.JsonObject headerObject){
      Map<String,String> headers = new Hashtable<>();
      for (String key: headerObject.keySet()){
        headers.put(key, headerObject.get(key).getAsString());
      }
      return headers;
    }

  public Model createModel(){
      com.google.gson.JsonObject modelObject = getJsonObjectFromMapTerm(modelTerm);
      System.out.println("model used: "+ modelObject.toString());
    ModelBuilder modelBuilder = new ModelBuilder();
    Resource interactionIdentifier = rdf.createBNode("interaction");
    if (agentUri.isPresent()){
      modelBuilder.add(interactionIdentifier, rdf.createIRI("http://example.org/interaction/hasAgent"), rdf.createIRI(agentUri.get()));
    }
    if (tdUrl.isPresent() && affordanceName.isPresent()){
      Resource tdAffordance = rdf.createBNode("tdAffordance");
      modelBuilder.add(interactionIdentifier, rdf.createIRI("http://example.org/interaction/useTDAffordance"), tdAffordance);
      modelBuilder.add(tdAffordance,  rdf.createIRI("http://example.org/interaction/hasTDUrl"), rdf.createIRI(tdUrl.get()));
      modelBuilder.add(tdAffordance, rdf.createIRI("http://example.org/interaction/hasAffordanceName"), rdf.createLiteral(affordanceName.get()));

    }
    Resource requestIdentifier = rdf.createBNode("request");
    modelBuilder.add(interactionIdentifier, rdf.createIRI("http://example.org/interaction/hasRequest"),requestIdentifier );
    String target = modelObject.get("request").getAsJsonObject().get("url").getAsString();
    modelBuilder.add(requestIdentifier, rdf.  createIRI("https://www.w3.org/2011/http#requestURI"), rdf.createIRI(target));
    String method =modelObject.get("request").getAsJsonObject().get("method").getAsString();
    modelBuilder.add(requestIdentifier, rdf.createIRI("https://www.w3.org/2011/http#methodName"), rdf.createLiteral(method));
    Map<String, String> requestHeaders = getHeaders( modelObject.get("request").getAsJsonObject().get("headers").getAsJsonObject());;
    for (String key: requestHeaders.keySet()){
      Resource headerIdentifier = rdf.createBNode("request header: "+key);
      String value = requestHeaders.get(key);
      modelBuilder.add(headerIdentifier, rdf.createIRI("https://www.w3.org/2011/http#fieldName"), rdf.createLiteral(key));
      modelBuilder.add(headerIdentifier, rdf.createIRI("https://www.w3.org/2011/http#fieldValue"), rdf.createLiteral(value));
      modelBuilder.add(requestIdentifier, rdf.createIRI("https://www.w3.org/2011/http#headers"), headerIdentifier);
    }
    JsonElement body = modelObject.get("request").getAsJsonObject().get("body");
    if (body != null) {
      modelBuilder.add(requestIdentifier, rdf.createIRI("http://example.org/interaction/hasBody"), rdf.createLiteral(body.getAsString()));
    }
    Resource responseIdentifier = rdf.createBNode("response");
    int statusCode = modelObject.get("response").getAsJsonObject().get("statusCode").getAsInt();
    Resource statusCodeId = rdf.createBNode("statusCode");
    modelBuilder.add(responseIdentifier, rdf.createIRI("https://www.w3.org/2011/http#sc"), statusCodeId);
    modelBuilder.add(statusCodeId, rdf.createIRI("https://www.w3.org/2011/http#statusCodeNumber"), rdf.createLiteral(statusCode));
    Map<String, String> responseHeaders = getHeaders(modelObject.get("response").getAsJsonObject().get("headers").getAsJsonObject());
    for (String key: responseHeaders.keySet()){
      Resource headerIdentifier = rdf.createBNode("response header: "+key);
      String value = responseHeaders.get(key);
      modelBuilder.add(headerIdentifier, rdf.createIRI("https://www.w3.org/2011/http#fieldName"), rdf.createLiteral(key));
      modelBuilder.add(headerIdentifier, rdf.createIRI("https://www.w3.org/2011/http#fieldValue"), rdf.createLiteral(value));
      modelBuilder.add(responseIdentifier, rdf.createIRI("https://www.w3.org/2011/http#headers"), headerIdentifier);
    }
    JsonElement payload= modelObject.get("request").getAsJsonObject().get("body");
    if (payload != null) {
      modelBuilder.add(requestIdentifier, rdf.createIRI("http://example.org/interaction/hasBody"), rdf.createLiteral(payload.getAsString()));
    }
    modelBuilder.add(interactionIdentifier, rdf.createIRI("http://example.org/interaction/hasReponse"),responseIdentifier );



    return modelBuilder.build();
  }

  public String cleanString(String str){
    String returnString = str;
    if (str.startsWith("\"")){
      returnString = str.substring(1, str.length()-1);
    }
    System.out.println("clean string: "+ returnString);
    return returnString;
  }

}
