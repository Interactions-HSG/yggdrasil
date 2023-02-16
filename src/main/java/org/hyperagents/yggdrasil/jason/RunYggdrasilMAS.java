package org.hyperagents.yggdrasil.jason;

import jason.architecture.AgArch;
import jason.architecture.MindInspectorAgArch;
import jason.infra.centralised.CentralisedAgArch;
import jason.infra.centralised.RunCentralisedMAS;
import jason.runtime.RuntimeServicesFactory;

public class RunYggdrasilMAS extends RunCentralisedMAS {

  public RunYggdrasilMAS(){
    super();
    this.dfAg = new MindInspectorAgArch();
    try {
      JasonAgent df = new JasonAgent("df");
      this.dfAg = df.getTS().getAgArch();
      System.out.println("df ag updated");
    } catch (Exception e){
      e.printStackTrace();
    }
    AgArch agArch = getDFAgArch();
    System.out.println("df ag: "+ agArch.getAgName());
  }

  /*@Override
  public void addAg(CentralisedAgArch agArch) throws IllegalArgumentException{
    if (agArch instanceof YggdrasilAgArch){
      super.addAg(agArch);
    } else {
      throw new IllegalArgumentException();
    }
  }*/

}
