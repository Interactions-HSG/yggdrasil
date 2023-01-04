package org.hyperagents.yggdrasil.jason;


import jason.JasonException;
import jason.asSemantics.Agent;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.PlanLibrary;
import jason.bb.DefaultBeliefBase;
import jason.mas2j.ClassParameters;
import jason.runtime.Settings;

import java.io.InputStream;

public class JasonAgent  extends Agent {
  private YggdrasilAgArch agentArch;

  public JasonAgent(YggdrasilAgArch agentArch, ClassParameters bbPars, String asSrc, Settings stts) throws JasonException {
    super();
    this.agentArch = agentArch;
    this.ts = new TransitionSystem(this, null, stts, agentArch);
    this.bb = new DefaultBeliefBase();
    this.pl = new PlanLibrary();
    this.initAg();
    this.load(asSrc);
  }

  public JasonAgent(String asSrc) throws JasonException {
    super();
    YggdrasilAgArch arch = new YggdrasilAgArch();
    this.agentArch = arch;
    Settings settings = new Settings();
    this.ts = new TransitionSystem(this, null, settings, arch);
    this.bb = new DefaultBeliefBase();
    this.pl = new PlanLibrary();
    this.initAg();
    this.load(asSrc);
  }

  public JasonAgent(String agentName, InputStream in, String sourceId) throws JasonException {
    super();
    System.out.println("is creating Jason agent");
    YggdrasilAgArch arch = new YggdrasilAgArch();
    //arch.insertAgArch(new YAgentArch());
    arch.insertAgArch(new YAgentArch2());
    System.out.println("Yggdrasil arch created");
    arch.setAgName(agentName);
    this.agentArch = arch;
    Settings settings = new Settings();
    this.ts = new TransitionSystem(this, null, settings, arch);
    this.bb = new DefaultBeliefBase();
    this.pl = new PlanLibrary();
    this.initAg();
    this.load(in, sourceId);
  }

}

