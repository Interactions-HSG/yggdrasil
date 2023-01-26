package org.hyperagents.yggdrasil.jason;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Hashtable;
import java.util.Map;

public class AgentRegistry {

  private static AgentRegistry registry;

  private String httpPrefix = "http://localhost:8080/"; //TODO: check

  private Map<String, String> agents;

  private Map<String, AgentNotificationCallback> callbacks;

  private Map<String, AgentMessageCallback> messageCallbackMap;

  private Map<ImmutablePair<String, String>, String> bodies;

  private AgentRegistry(){
    this.agents = new Hashtable<>();
    this.callbacks = new Hashtable<>();
    this.messageCallbackMap = new Hashtable<>();
    this.bodies = new Hashtable<>();
  }

  public static AgentRegistry getInstance(){
    if (registry == null){
      registry = new AgentRegistry();
    }
    return registry;

  }



  public void printAllAgents(){
    for (String agent: agents.keySet()){
      System.out.println(agent);
      System.out.println(agents.get(agent));
    }
  }

  public String addAgent(String agentName) throws Exception {
    String agentUri = httpPrefix + "agents/"+agentName;
    System.out.println("agent uri: "+ agentUri);
    if (!agents.containsKey(agentName)) {
      this.agents.put(agentName, agentUri);
      this.callbacks.put(agentName, new AgentNotificationCallback(agentUri));
      this.messageCallbackMap.put(agentName, new AgentMessageCallback(agentName));

      return agentName;
    }
    else {
      throw new Exception("Agent already exists");
    }
  }

  public String getAgentUri(String agentName) throws Exception {
    if (agents.containsKey(agentName)){
      return agents.get(agentName);
    } else {
      throw new Exception("Agent does not exist");
    }
  }

  public AgentNotificationCallback getAgentCallback(String agentName) throws Exception {
    if (callbacks.containsKey(agentName)){
      return callbacks.get(agentName);
    } else {
      throw new Exception("Agent does not exist");
    }
  }

  public AgentMessageCallback getAgentMessageCallback(String agentName) throws Exception {
    if (messageCallbackMap.containsKey(agentName)){
      return messageCallbackMap.get(agentName);
    } else {
      throw new Exception("Agent does not exist");
    }
  }

   void addBody(String agentName, String workspaceName, String bodyName){
    this.bodies.put(new ImmutablePair(agentName, workspaceName), bodyName);
  }

  public String getBody(String agentName, String workspaceName){
    return this.bodies.get(new ImmutablePair<>(agentName, workspaceName));
  }

   void removeBody(String agentName, String workspaceName){
    this.bodies.remove(new ImmutablePair<>(agentName, workspaceName));
  }

  public String getHttpPrefix(){
    return httpPrefix;
  }
}
