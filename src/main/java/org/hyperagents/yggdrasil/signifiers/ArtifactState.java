package org.hyperagents.yggdrasil.signifiers;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;

public class ArtifactState {

  Resource artifactId;

  Model model;

  public ArtifactState(Resource artifactId, Model model){
    this.artifactId = artifactId;
    this.model = model;
  }

  public Resource getArtifactId(){
    return artifactId;
  }

  public Model getModel(){
    return model;
  }
}
