package org.hyperagents.yggdrasil.signifiers.maze;

import cartago.AgentId;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Models;
import org.hyperagents.signifier.Signifier;
import org.hyperagents.util.RDFS;
import org.hyperagents.yggdrasil.signifiers.AgentProfile;
import org.hyperagents.yggdrasil.signifiers.SignifierHypermediaArtifact;
import org.hyperagents.yggdrasil.signifiers.Visibility;
import org.hyperagents.yggdrasil.signifiers.maze.scripts.ConveyOntology;

import java.util.Set;

public class VisibilityConvey2 implements Visibility {
  @Override
  public boolean isVisible(Signifier signifier, Model artifactState, AgentProfile profile) {
    boolean b = false;
    int n = countFree(artifactState);
    if (n==0){
      b = true;
      System.out.println("order milk is visible");
    }
    System.out.println("order milk is invisible");
    return b;
  }

  public int countFree(Model model){
    boolean b = false;
    Set<Resource> itemIds = Models.objectResources(model.filter(RDFS.rdf.createIRI(ConveyOntology.thisArtifact),
      RDFS.rdf.createIRI(ConveyOntology.hasFree), null));
    int n = itemIds.size();
    return n;
  }

  public boolean isVisible1(Signifier signifier, SignifierHypermediaArtifact artifact, AgentId agent) {
    boolean b = false;
    ConveyingWorkshop workshop = (ConveyingWorkshop) artifact;
    int n = workshop.countFree();
    if (n==0){
      b = true;
    }
    return b;
  }
}
