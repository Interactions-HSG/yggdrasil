package org.hyperagents.yggdrasil.jason;

import jason.infra.centralised.CentralisedAgArch;
import jason.infra.centralised.RunCentralisedMAS;

public class RunYggdrasilMAS extends RunCentralisedMAS {

  @Override
  public void addAg(CentralisedAgArch agArch) throws IllegalArgumentException{
    if (agArch instanceof YggdrasilAgArch){
      super.addAg(agArch);
    } else {
      throw new IllegalArgumentException();
    }
  }

}
