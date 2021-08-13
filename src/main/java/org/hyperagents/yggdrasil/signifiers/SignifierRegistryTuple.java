package org.hyperagents.yggdrasil.signifiers;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.hyperagents.signifier.Signifier;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifact;

public class SignifierRegistryTuple {

  private Signifier signifier;
  private Visibility visibility;
  private HypermediaArtifact artifact;

  public void SignifierRegistryTuple(Signifier signifier, Visibility visibility, HypermediaArtifact artifact){
    this.signifier = signifier;
    this.visibility = visibility;
    this.artifact = artifact;
  }

  public Signifier getSignifier(){
    return this.signifier;
  }

  public String getSignifierContent(){
    return this.signifier.getTextTriples(RDFFormat.TURTLE);
  }

  public Visibility getVisibility(){
    return this.visibility;
  }

  public HypermediaArtifact getArtifact(){
    return this.artifact;
  }
}
