package org.hyperagents.yggdrasil.jason;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Hashtable;
import java.util.Map;

public class AgentRegistry {

  private static AgentRegistry registry;

  private String httpPrefix = "http://localhost:8080/"; //TODO: check

  private Map<String, String> agents;

  private Map<String, AgentNotificationCallback> callbacks;

  private Map<String, AgentMessageCallback> messageCallbackMap;

  private Map<String, AgentJasonMessageCallback> jasonMessageCallbackMap;

  private Map<ImmutablePair<String, String>, String> bodies;

  private AgentRegistry(){
    this.agents = new Hashtable<>();
    this.callbacks = new Hashtable<>();
    this.messageCallbackMap = new Hashtable<>();
    this.jasonMessageCallbackMap = new Hashtable<>();
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
      this.callbacks.put(agentName, new AgentNotificationCallback());
      this.messageCallbackMap.put(agentName, new AgentMessageCallback());
      this.jasonMessageCallbackMap.put(agentName, new AgentJasonMessageCallback());

      return agentName;
    }
    else {
      throw new Exception("Agent already exists");
    }
  }

  public void deleteAgent(String agentName){
    if (agents.containsKey(agentName)) {
      agents.remove(agentName);
      this.callbacks.remove(agentName);
      this.messageCallbackMap.remove(agentName);
      this.jasonMessageCallbackMap.remove(agentName);

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

  public AgentJasonMessageCallback getAgentJasonMessageCallback(String agentName) throws Exception {
    if (jasonMessageCallbackMap.containsKey(agentName)){
      return jasonMessageCallbackMap.get(agentName);
    } else {
      throw new Exception("Agent does not exist");
    }
  }

  public void setHttpPrefix(JsonObject config) {
    JsonObject httpConfig = config.getJsonObject("http-config");
    if (httpConfig.containsKey("base-uri")) {
      this.httpPrefix = httpConfig.getString("base-uri");
    } else {
      String host = "localhost";
      if (httpConfig.containsKey("host")) {
        host = httpConfig.getString("host");
      }
      int port = 8080;
      if (httpConfig.containsKey("port")) {
        port = httpConfig.getInteger("port");
      }
      this.httpPrefix = "http://" + host + ":" + port + "/";
      System.out.println("http prefix: "+ httpPrefix);
    }
  }
}
