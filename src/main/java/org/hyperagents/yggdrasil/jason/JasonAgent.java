package org.hyperagents.yggdrasil.jason;


import jason.JasonException;
import jason.architecture.MindInspectorAgArch;
import jason.asSemantics.Agent;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.PlanLibrary;
import jason.bb.DefaultBeliefBase;
import jason.infra.centralised.CentralisedAgArch;
import jason.runtime.Settings;

import java.io.InputStream;

public class JasonAgent  extends Agent {

  public JasonAgent(String agentName, InputStream in, String sourceId) throws JasonException {
    super();
    System.out.println("is creating Jason agent");
    CentralisedAgArch arch = new CentralisedAgArch();
    arch.insertAgArch(new YAgentArch());
    arch.insertAgArch(new MindInspectorAgArch());
    System.out.println("Yggdrasil arch created");
    arch.setAgName(agentName);
    Settings settings = new Settings();
    this.ts = new TransitionSystem(this, null, settings, arch);
    this.bb = new DefaultBeliefBase();
    this.pl = new PlanLibrary();
    this.initAg();
    this.load(in, sourceId);
  }

  public JasonAgent(String agentName) throws JasonException {
    super();
    System.out.println("is creating Jason agent");
    CentralisedAgArch arch = new CentralisedAgArch();
    arch.insertAgArch(new YAgentArch());
    arch.insertAgArch(new MindInspectorAgArch());
    System.out.println("Yggdrasil arch created");
    arch.setAgName(agentName);
    Settings settings = new Settings();
    this.ts = new TransitionSystem(this, null, settings, arch);
    this.bb = new DefaultBeliefBase();
    this.pl = new PlanLibrary();
    this.initAg();
  }

}

