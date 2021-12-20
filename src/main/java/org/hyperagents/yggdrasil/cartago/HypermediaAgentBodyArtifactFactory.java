package org.hyperagents.yggdrasil.cartago;

import cartago.Artifact;
import cartago.ArtifactFactory;
import cartago.CartagoException;

public class HypermediaAgentBodyArtifactFactory extends ArtifactFactory {

  public HypermediaAgentBodyArtifactFactory(){
    super("hypermediaAgentBodyArtifactFactory");
  }

  @Override
  public Artifact createArtifact(String templateName) throws CartagoException {
    try{
      if (templateName == HypermediaBodyArtifact.class.getName()){
        HypermediaBodyArtifact artifact = new HypermediaBodyArtifact();
        return artifact;
      }
      else {
        throw new CartagoException("Is not HypermediaAgentBodyArtifact");
      }
    } catch (Exception e){
      throw new CartagoException("Is not HypermediaAgentBodyArtifact");
    }
  }
}
