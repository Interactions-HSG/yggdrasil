package org.hyperagents.yggdrasil.signifiers;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpResponse;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.schemas.IntegerSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.hyperagents.io.SignifierReader;
import org.hyperagents.signifier.Signifier;
import org.hyperagents.util.RDFS;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifact;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public abstract class SignifierHypermediaArtifact extends HypermediaArtifact {

  protected SignifierRegistry registry = SignifierRegistry.getInstance();

  protected Map<String, IRI> agentProfiles = new HashMap<String, IRI>();

  private static final String WEBID_PREFIX = "http://hyperagents.org/";

  public abstract Model getState();


  public AgentProfile getAgentProfile(String agentName) {
    ValueFactory rdf = SimpleValueFactory.getInstance();
    //Resource agentId = rdf.createIRI("http://example.com/thisAgent");
    Resource agentId = rdf.createIRI(agentName);
    AgentProfile profile = new AgentProfile(agentId);
    if (agentProfiles.containsKey(agentName)) {
      System.out.println("agent profiles contains key");
      IRI agentProfileIRI = agentProfiles.get(agentName);
      profile = retrieveAgentProfile(agentProfileIRI);

    }
    System.out.println("profile retrieved: "+profile);
    return profile;
  }

  /*public AgentProfile retrieveAgentProfile(IRI agentProfileIRI) {
    ValueFactory rdf = SimpleValueFactory.getInstance();
    Resource agentId = rdf.createIRI("http://example.com/thisAgent");
    AgentProfile profile = new AgentProfile(agentId);
    String uri = agentProfileIRI.toString()+"/profile";
    System.out.println("uri: "+uri);
    Map<String, String> headers = getStandardHeaders(true);
    String method = "POST";
    System.out.println("before request");
    String reply = sendRequestReturn(uri, method, headers, Optional.empty());
    System.out.println("after request");
    System.out.println("reply: "+reply);
    profile = AgentProfile.parse(reply);
    System.out.println("profile retrieved");
    return profile;


  }*/

  public AgentProfile retrieveAgentProfile(IRI agentProfileIRI){
    AgentProfile profile = null;
    String profileUrl = agentProfileIRI.toString();
    System.out.println("profile url: "+profileUrl);
    String str = retrieveProfileFromArtifact(profileUrl);
    System.out.println("profile retrieved: "+str);
    profile = AgentProfile.parse(str);
    return profile;

  }

  /*public AgentProfile retrieveAgentProfile(IRI agentProfileIRI) {
    ValueFactory rdf = SimpleValueFactory.getInstance();
    Resource agentId = rdf.createIRI("http://example.com/thisAgent");
    AgentProfile profile = new AgentProfile(agentId);
    String uri = agentProfileIRI.toString()+"/profile";
    System.out.println("uri: "+uri);
    BasicClassicHttpRequest request = new BasicClassicHttpRequest("POST", uri);
    request.addHeader("Content-Type","application/json");
    String artifactWebId = WEBID_PREFIX + getArtifactName();
    System.out.println("artifact web id: "+artifactWebId);
    request.addHeader("X-Agent-WebId",artifactWebId);
    request.addHeader("X-Reply","true");
    HttpClient client = HttpClients.createDefault();
    try {
      ClassicHttpResponse response = (ClassicHttpResponse) client.execute(request);
      HttpEntity entity = response.getEntity();
      System.out.println(entity);
      String encoding = entity.getContentEncoding() == null ? "UTF-8" : entity.getContentEncoding();
      System.out.println("encoding: "+encoding);
      String payloadContent = IOUtils.toString(entity.getContent(), encoding);
      System.out.println("payload content");
      System.out.println(payloadContent);
      System.out.println("end payload content");
      profile = AgentProfile.parse(payloadContent);
    } catch(Exception e){
      e.printStackTrace();
    }
    return profile;

  }*/


  @OPERATION
  public void addSignifier(String signifierName, Signifier signifier){
    Visibility visibility = new VisibilityImpl();
    //Tuple t = new Tuple(signifierName, signifier, visibility, this );
    SignifierRegistryTuple t = new SignifierRegistryTuple(signifier, visibility, this);
    registry.addSignifier(RDFS.rdf.createIRI(signifierName),t);
  }

  public void registerSignifier(Signifier signifier, Visibility v){
    System.out.println(signifier.getTextTriples(RDFFormat.TURTLE));
    SignifierRegistryTuple tuple = new SignifierRegistryTuple(signifier, v, this);
    this.registry.addSignifier(tuple);
  }

  @OPERATION
  public void registerProfile(String profileUri, int useless){
    IRI profileIri = RDFS.rdf.createIRI(profileUri);
    this.agentProfiles.put(this.getCurrentOpAgentId().getAgentName(), profileIri);
    System.out.println(agentProfiles);
  }

  @OPERATION
  public void addSignifier(Signifier signifier){
    Visibility visibility = new VisibilityImpl();
    SignifierRegistryTuple t = new SignifierRegistryTuple(signifier, visibility, this);
    registry.addSignifier(t);
  }

  @OPERATION
  public void addSignifierContent(String content, int useless){
    Signifier signifier = SignifierReader.readSignifier(content, RDFFormat.TURTLE);
    Visibility visibility = new VisibilityImpl();
    SignifierRegistryTuple t = new SignifierRegistryTuple(signifier, visibility, this);
    registry.addSignifier(t);
  }


  @OPERATION
  public void isVisible(String agentName, String signifierUri, OpFeedbackParam<Object> returnParam){
    boolean b = this.registry.isVisible(agentName, signifierUri);
    returnParam.set(b);
  }

  /*@OPERATION
  public void retrieveVisibleSignifiers(String agentName, OpFeedbackParam<Object> returnParam){
    Set<IRI> visibles = new HashSet<>();
    Set<IRI> artifactSignifiers = registry.getArtifactSignifiers(this);
    for (IRI signifierId : artifactSignifiers){
      if (registry.isVisible(agentName, signifierId.toString())){
        visibles.add(signifierId);
      }
    }
    List<IRI> signifiers = new Vector<>(visibles);
    returnParam.set(signifiers);

  }*/

  @OPERATION
  public void retrieveVisibleSignifiers(OpFeedbackParam<Object> returnParam){
    String agentName = this.getCurrentOpAgentId().getAgentName();
    System.out.println("agent name: "+agentName);
    Set<IRI> visibles = new HashSet<>();
    Set<IRI> artifactSignifiers = registry.getArtifactSignifiers(this);
    for (IRI signifierId : artifactSignifiers){
      if (registry.isVisible(agentName, signifierId.toString())){
        visibles.add(signifierId);
      }
    }
    List<IRI> signifierList = new Vector<>(visibles);
    String signifiers = signifierList.toString();
    System.out.println(signifiers);
    returnParam.set(signifiers);

  }

  @OPERATION
  public void retrieveSignifier(String signifierUrl, OpFeedbackParam<Object> returnParam){
    String agentName = this.getCurrentOpAgentId().getAgentName();
    String signifierContent = registry.getSignifierfromUri(agentName, signifierUrl);
    returnParam.set(signifierContent);

  }

  private Method getMethodFromList(List<Method> methods, String methodName){
    Method method = null;
    for (Method m: methods){
      if (m.getName().equals(methodName)){
        method=m;
      }
    }
    return method;
  }

  @OPERATION
  public void useOperation(String operation, Object[] params){
    Class signifierClass = this.getClass();
    try {
      Method[] methods = signifierClass.getMethods();
      List<Method> methodList = Arrays.asList(methods);
      for (Method method : methodList){
        //System.out.println("Method Name : "+method.getName());
      }
      Method method = getMethodFromList(methodList, operation);
      if (isOperation(method)) {
        method.invoke(this, params);
      } else {
        //System.out.println("Method invoked is not an operation");
        throw new Exception("Method invoked is not an operation");
      }
      //Method method = signifierClass.getMethod(operation);
      //method.invoke(this,params);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

  private boolean isOperation(Method m){
    boolean b = false;
    Annotation[] annotations = m.getAnnotations();
    for (Annotation annotation : annotations){
      Class atype = annotation.annotationType();
      if (atype.equals(OPERATION.class)){
        System.out.println("is operation");
        b = true;
      }
    }
    return b;
  }

  public void sendRequest(String urlString, String method, Map<String, String> headers, Optional<String> payload){
    BasicClassicHttpRequest request = new BasicClassicHttpRequest(method, urlString);
    for (String key : headers.keySet()){
      String value = headers.get(key);
      request.addHeader(key, value);
    }
    if (payload.isPresent()){
      request.setEntity(new StringEntity(payload.get()));
    }
    HttpClient client = HttpClients.createDefault();
    try {
      client.execute(request);
    } catch (Exception e){
      e.printStackTrace();
    }
  }

  public String sendRequestReturn1(String urlString, String method, Map<String, String> headers, Optional<String> payload){
    BasicClassicHttpRequest request = new BasicClassicHttpRequest(method, urlString);
    for (String key : headers.keySet()){
      String value = headers.get(key);
      request.addHeader(key, value);
    }
    if (payload.isPresent()){
      request.setEntity(new StringEntity(payload.get()));
    }
    HttpClient client = HttpClients.createDefault();
    try {
      ClassicHttpResponse response = (ClassicHttpResponse) client.execute(request);
      HttpEntity entity = response.getEntity();
      System.out.println(entity);
      String encoding = entity.getContentEncoding() == null ? "UTF-8" : entity.getContentEncoding();
      System.out.println("encoding: "+encoding);
      String payloadContent = IOUtils.toString(entity.getContent(), encoding);
      System.out.println("payload content");
      System.out.println(payloadContent);
      System.out.println("end payload content");
      return payloadContent;
    } catch(Exception e){
      e.printStackTrace();
    }
    return null;
  }

  public String sendRequestReturn(String urlString, String method, Map<String, String> headers, Optional<String> payload){
    String str = "";
    BasicClassicHttpRequest request = new BasicClassicHttpRequest(method, urlString);
    for (String key : headers.keySet()){
      String value = headers.get(key);
      request.addHeader(key, value);
    }
    if (payload.isPresent()){
      request.setEntity(new StringEntity(payload.get()));
    }
    HttpClient client = HttpClients.createDefault();
    try {
      TDHttpResponse response = new TDHttpResponse((ClassicHttpResponse) client.execute(request));
      Optional<String> returnPayload = response.getPayload();
      if (returnPayload.isPresent()){
        System.out.println("return payload is present");
        str = returnPayload.get();
        System.out.println("string returned: "+str);
      }
    } catch(Exception e){
      System.out.println("There has been an exception");
      e.printStackTrace();
    }
    return str;
  }

  public Map<String, String> getStandardHeaders(boolean b){
    Map<String, String> headers = new HashMap<>();
    String artifactWebId = WEBID_PREFIX + getArtifactName();
    headers.put("X-Agent-WebId",artifactWebId);
    //headers.put("Content-Type","application/json");
    headers.put("Accept-Encoding", "text/turtle");
    if (b) {
      headers.put("X-Reply", "true");
    } else {
      headers.put("X-Reply", "false");
    }
    return headers;
  }

  public String retrieveProfileFromArtifact(String profileUrl){
    String str = "";
    String operationUrl = profileUrl+"/profile";
    System.out.println("operation url: "+operationUrl);
    try {
      URL url = new URL(operationUrl);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      Map<String, String> headers = getStandardHeaders(true);
      for (String key : headers.keySet()) {
        String value = headers.get(key);
        connection.addRequestProperty(key, value);
      }
      int responseCode = connection.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) { // success
        BufferedReader in = new BufferedReader(new InputStreamReader(
          connection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }
        in.close();

        // print result
        str = response.toString();
        System.out.println("response: "+str);
      }
    } catch(Exception e){
      e.printStackTrace();
    }
    return str;
  }



  protected void registerSignifierAffordances(){
    registerActionAffordance("http://example.org/retrieve", "retrieveVisibleSignifiers", "/retrieve");
    registerActionAffordance("http://example.org/retrievesignifier", "retrieveSignifier", "/retrievesignifier");
    DataSchema addSchema = new ArraySchema.Builder()
      .addItem(new StringSchema.Builder().build())
      .addItem(new IntegerSchema.Builder().build())
      .build();
    registerActionAffordance("http://example.org/add", "addSignifierContent", "/addsignifier", addSchema);
    DataSchema profileSchema = new ArraySchema.Builder()
      .addItem(new StringSchema.Builder().build())
      .addItem(new IntegerSchema.Builder().build())
      .build();
    registerActionAffordance("http://example.org/registerProfile", "registerProfile", "/profile", profileSchema);
  }




}

