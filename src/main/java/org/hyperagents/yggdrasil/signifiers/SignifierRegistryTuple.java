package org.hyperagents.yggdrasil.signifiers;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.hyperagents.signifier.Signifier;

public class SignifierRegistryTuple {

  private Signifier signifier;
  private Visibility visibility;
  private SignifierHypermediaArtifact artifact;

  public SignifierRegistryTuple(Signifier signifier, Visibility visibility, SignifierHypermediaArtifact artifact){
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

  public SignifierHypermediaArtifact getArtifact(){
    return this.artifact;
  }
}
