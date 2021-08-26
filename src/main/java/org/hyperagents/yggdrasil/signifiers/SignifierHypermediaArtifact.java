package org.hyperagents.yggdrasil.signifiers;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.hyperagents.signifier.Signifier;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifact;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

public abstract class SignifierHypermediaArtifact extends HypermediaArtifact {

  protected SignifierRegistry registry = SignifierRegistry.getInstance();

  protected Map<String, IRI> agentProfiles = new HashMap<String, IRI>();

  public void init(){

  }

public abstract Model getState();


  public AgentProfile getAgentProfile(String agentName) {
    ValueFactory rdf = SimpleValueFactory.getInstance();
    //Resource agentId = rdf.createIRI("http://example.com/thisAgent");
    Resource agentId = rdf.createIRI(agentName);
    AgentProfile profile = new AgentProfile(agentId);
    if (agentProfiles.containsKey(agentName)) {
      IRI agentProfileIRI = agentProfiles.get(agentName);
      profile = retrieveAgentProfile(agentProfileIRI);

    }
    return profile;
  }

  public AgentProfile retrieveAgentProfile(IRI agentProfileIRI) {
    ValueFactory rdf = SimpleValueFactory.getInstance();
    Resource agentId = rdf.createIRI("http://example.com/thisAgent");
    AgentProfile profile = new AgentProfile(agentId);
    return profile;

  }


  /*@OPERATION
  public void addSignifier(String signifierName, Signifier signifier){
    Visibility visibility = new VisibilityImpl();
    //Tuple t = new Tuple(signifierName, signifier, visibility, this );
    SignifierRegistryTuple t = new SignifierRegistryTuple(signifier, visibility, this);
    registry.addSignifier(RDFS.rdf.createIRI(signifierName),t);
  }*/

  @OPERATION
  public void addSignifier(Signifier signifier){
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

  protected void registerSignifierAffordances(){
    registerActionAffordance("http://example.org/retrieve", "retrieveVisibleSignifiers", "/retrieve");
    registerActionAffordance("http://example.org/retrievesignifier", "retrieveSignifier", "/retrievesignifier");
    registerActionAffordance("http://example.org/add", "addSignifier", "/addsignifier");
  }




}

