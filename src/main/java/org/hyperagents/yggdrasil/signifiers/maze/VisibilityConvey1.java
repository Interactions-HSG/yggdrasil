package org.hyperagents.yggdrasil.signifiers.maze;


import cartago.AgentId;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Models;
import org.hyperagents.hypermedia.HypermediaPlan;
import org.hyperagents.signifier.Signifier;
import org.hyperagents.util.Plan;
import org.hyperagents.util.RDFS;
import org.hyperagents.yggdrasil.signifiers.AgentProfile;
import org.hyperagents.yggdrasil.signifiers.SignifierHypermediaArtifact;
import org.hyperagents.yggdrasil.signifiers.Visibility;
import org.hyperagents.yggdrasil.signifiers.maze.scripts.ConveyOntology;

import java.util.Optional;
import java.util.Set;

public class VisibilityConvey1 implements Visibility {

  public boolean isVisible1(Signifier signifier, SignifierHypermediaArtifact artifact, AgentId agent) {
    boolean b = false;
    ConveyingWorkshop workshop = (ConveyingWorkshop) artifact;
    if (!workshop.isWaiting()){
      int i = getParameter1(signifier);
      int j = getParameter2(signifier);
      if (workshop.isFree(i,j)){
        b = true;
      }
    }
    return b;
  }

  private int getParameter1(Signifier signifier){
    int parameter1 = 0;
    System.out.println("start getParameter1");
    System.out.println("signifierId: "+signifier.getSignifierId());
    System.out.println(signifier.getAffordanceList());
    Plan plan = signifier.getAffordanceList().get(0).getFirstPlan();
    System.out.println("hasFirstPlan");
    HypermediaPlan hypermediaPlan = HypermediaPlan.getAsHypermediaPlan(plan);
    Optional<String> payload = hypermediaPlan.getPayload();
    if (payload.isPresent()){
      System.out.println("payload present");
      String str = payload.get();
      System.out.println("payload: "+payload);
      JsonElement e = JsonParser.parseString(str);
      JsonArray array = e.getAsJsonArray();
      System.out.println(array);
      parameter1 = array.get(0).getAsInt();
    }
    return parameter1;
  }

  private int getParameter2(Signifier signifier){
    int parameter2 = 0;
    Plan plan = signifier.getAffordanceList().get(0).getFirstPlan();
    HypermediaPlan hypermediaPlan = HypermediaPlan.getAsHypermediaPlan(plan);
    Optional<String> payload = hypermediaPlan.getPayload();
    if (payload.isPresent()){
      String str = payload.get();
      JsonElement e = JsonParser.parseString(str);
      JsonArray array = e.getAsJsonArray();
      parameter2 = array.get(1).getAsInt();
    }
    return parameter2;
  }

  public boolean isWaiting(Model model){
  boolean b = false;
  Optional<Literal> opLit = Models.objectLiteral(model.filter(RDFS.rdf.createIRI(ConveyOntology.thisArtifact),
    RDFS.rdf.createIRI(ConveyOntology.isWaiting), null));
  if (opLit.isPresent()){
    b = opLit.get().booleanValue();
  }
  return b;
  }

  public boolean isFree(Model model, int i, int j){
    boolean b = false;
    Set<Resource> itemIds = Models.objectResources(model.filter(RDFS.rdf.createIRI(ConveyOntology.thisArtifact),
      RDFS.rdf.createIRI(ConveyOntology.hasFree), null));
    for (Resource itemId : itemIds){
      Optional<Literal> opX = Models.objectLiteral(model.filter(itemId,
        RDFS.rdf.createIRI(ConveyOntology.hasX),null));
      Optional<Literal> opZ = Models.objectLiteral(model.filter(itemId,
        RDFS.rdf.createIRI(ConveyOntology.hasZ),null));
      if (opX.isPresent() && opZ.isPresent()){
        int x = opX.get().intValue();
        int z = opZ.get().intValue();
        if (x == i && z == j){
          b = true;
        }
      }
    }
    return b;
  }

  @Override
  public boolean isVisible(Signifier signifier, Model artifactState, AgentProfile profile) {
    boolean b = false;

    if (!isWaiting(artifactState)){
      int i = getParameter1(signifier);
      int j = getParameter2(signifier);
      if (isFree(artifactState, i, j)){
        b = true;
        System.out.println("is visible: "+i+" "+j);
      }
      else {
        System.out.println("not is visible: "+i+" "+j);
      }
    }
    return b;

  }
}

