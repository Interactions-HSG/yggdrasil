package org.hyperagents.yggdrasil.jason;

import jason.asSemantics.Agent;
import jason.infra.centralised.CentralisedRuntimeServices;
import jason.mas2j.ClassParameters;
import jason.runtime.Settings;

import java.io.InputStream;
import java.util.Collection;

public class YggdrasilRuntimeServices extends CentralisedRuntimeServices {

  public YggdrasilRuntimeServices(RunYggdrasilMAS masRunner){
    super(masRunner);
  }

  @Override
  public String createAgent(String agName, String agSource, String agClass, Collection<String> archClasses, ClassParameters bbPars, Settings stts, Agent father) throws Exception {
    Agent agent = father;
    YggdrasilAgArch agArch = (YggdrasilAgArch) agent.getTS().getUserAgArch();
    masRunner.addAg(agArch);
    return agName;
  }

  public String createAgent(String agentName, String aslFile){
    try {
      System.out.println("create agent from file");
      JasonAgent agent = new JasonAgent(aslFile);
      YggdrasilAgArch agArch = (YggdrasilAgArch) agent.getTS().getUserAgArch();
      masRunner.addAg(agArch);
      return agArch.getAgName();
    } catch(Exception e){
      e.printStackTrace();
    }
    return null;

  }

  public String createAgent(String agentName, InputStream in, String sourceId){
    try {
      JasonAgent agent = new JasonAgent(agentName, in, sourceId);
      YggdrasilAgArch agArch = (YggdrasilAgArch) agent.getTS().getUserAgArch();
      masRunner.addAg(agArch);
      return agArch.getAgName();
    } catch(Exception e){
      e.printStackTrace();
    }
    return null;

  }
}
