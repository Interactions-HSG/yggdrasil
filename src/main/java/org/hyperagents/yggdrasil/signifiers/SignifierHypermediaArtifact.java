package org.hyperagents.yggdrasil.signifiers;

import cartago.AgentId;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifact;

import java.lang.reflect.Method;
import java.util.*;

public abstract class SignifierHypermediaArtifact extends HypermediaArtifact {

  private SignifierRegistry registry = SignifierRegistry.getInstance();

  protected Map<String, IRI> agentProfiles=new HashMap<String, IRI>();

  public abstract ArtifactState getState();

  public abstract AgentProfile getAgentProfile(AgentId agent);

  public AgentProfile getAgentProfile(String agentName){
    ValueFactory rdf = SimpleValueFactory.getInstance();
    Resource agentId = rdf.createIRI("http://example.com/thisAgent");
    AgentProfile profile = new AgentProfile(agentId);
    if (agentProfiles.containsKey(agentName)){
      IRI agentProfileIRI = agentProfiles.get(agentName);
      profile = retrieveAgentProfile(agentProfileIRI);

    }
    return profile;
  }

  public AgentProfile retrieveAgentProfile(IRI agentProfileIRI){
    ValueFactory rdf = SimpleValueFactory.getInstance();
    Resource agentId = rdf.createIRI("http://example.com/thisAgent");
    AgentProfile profile = new AgentProfile(agentId);
    return profile;

  }

  @OPERATION
  public void useOperation(String operation, Object[] params){
    Class signifierClass = this.getClass();
    try {
      Method[] methods = signifierClass.getDeclaredMethods();
      List<Method> methodList = Arrays.asList(methods);
      for (Method method : methodList){
        //System.out.println("Method Name : "+method.getName());
      }
      Method method = getMethodFromList(methodList, operation);
      method.invoke(this,params);
      //Method method = signifierClass.getMethod(operation);
      //method.invoke(this,params);
    }
    catch(Exception e){
      e.printStackTrace();
    }
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
  public void isVisible(String agentName, String signifierUri, OpFeedbackParam<Object> returnParam){
    boolean b = this.registry.isVisible(agentName, signifierUri);
    returnParam.set(b);
  }

  @OPERATION
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

  }


}
