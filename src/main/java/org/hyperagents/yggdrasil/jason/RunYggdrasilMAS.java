package org.hyperagents.yggdrasil.jason;

import jason.architecture.AgArch;
import jason.architecture.MindInspectorAgArch;
import jason.infra.centralised.RunCentralisedMAS;

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

}
