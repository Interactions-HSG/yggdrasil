package org.hyperagents.yggdrasil.jason;

import java.util.Hashtable;
import java.util.Map;

public class AgentRegistry {

  private static AgentRegistry registry;

  private String httpPrefix = "http://localhost:8080/";

  private Map<String, String> agents;

  private AgentRegistry(){
    this.agents = new Hashtable<>();
  }

  public static AgentRegistry getInstance(){
    if (registry == null){
      registry = new AgentRegistry();
    }
    return registry;

  }

  public String addAgent(String agentName) throws Exception {
    String agentUri = httpPrefix + agentName;
    if (!agents.containsKey(agentName)) {
      this.agents.put(agentName, agentUri);
      return agentUri;
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
}
