package org.hyperagents.yggdrasil.jason;

import jason.asSemantics.Agent;
import jason.infra.centralised.CentralisedAgArch;
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
  public String createAgent(String agName, String agSource, String agClass, Collection<String> archClasses, ClassParameters bbPars, Settings stts, Agent father) {
    CentralisedAgArch agArch = (CentralisedAgArch) father.getTS().getAgArch();
    masRunner.addAg(agArch);
    return agName;
  }
  public String createAgent(String agentName, InputStream in, String sourceId){
    try {
      JasonAgent agent = new JasonAgent(agentName, in, sourceId);
      CentralisedAgArch agArch = (CentralisedAgArch) agent.getTS().getAgArch().getNextAgArch().getNextAgArch();
      masRunner.addAg(agArch);
      return agArch.getAgName();
    } catch(Exception e){
      e.printStackTrace();
    }
    return null;

  }
}
