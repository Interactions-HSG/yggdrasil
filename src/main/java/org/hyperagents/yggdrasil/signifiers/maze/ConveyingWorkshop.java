package org.hyperagents.yggdrasil.signifiers.maze;

import cartago.OPERATION;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.schemas.IntegerSchema;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.hyperagents.util.RDFS;
import org.hyperagents.yggdrasil.signifiers.SignifierHypermediaArtifact;
import org.hyperagents.yggdrasil.signifiers.maze.scripts.ConveyOntology;

public class ConveyingWorkshop extends SignifierHypermediaArtifact {

  int n;
  int m;
  PositionMap map;
  boolean wait;

  public void init(int n, int m){
    this.n = n;
    this.m = m;
    this.map = new PositionMap();
    for (int i = 0; i<n; i++){
      for (int j = 0; j<m;j++){
        this.map.add(i, j, true);

      }
    }
    this.wait = false;
    createSignifiers(n,m);

  }

  @OPERATION
  public void pickItem(int i, int j){
    if (this.map.hasKeys(i,j)){
      this.map.add(i, j, false);
    }
  }

  @OPERATION
  public void orderMilk(){
    this.wait = true;
    for (int i = 0; i < this.n; i++){
      for (int j = 0; j< this.m; j++){
        this.map.add(i,j, true);
      }
    }
    this.wait = false;
  }

  public boolean isWaiting(){
    return this.wait;
  }

  public PositionMap getMap(){
    return this.map;
  }

  public boolean isFree(int i, int j){
    boolean b = map.get(i,j);
    return b;
  }

  public int countFree(){
    int n = 0;
    for (int i = 0;i<this.n;i++){
      for (int j = 0; j<this.m;j++){
        boolean b = this.map.get(i,j);
        if (b){
          n++;
        }
      }
    }
    return n;
  }

  private void createSignifiers(int n, int m){
    for (int i = 0; i< n; i++){
      for (int j = 0; j< m; j++){
        createOneSignifier(i,j);
      }
    }
    createEmptySignifier();
  }

  public void createOneSignifier(int i, int j){

  }

  public void createEmptySignifier(){

  }

  @Override
  public void registerInteractionAffordances(){
    registerSignifierAffordances();
    DataSchema pickSchema = new ArraySchema.Builder()
      .addItem(new IntegerSchema.Builder().build())
      .addItem(new IntegerSchema.Builder().build())
      .build();
    registerActionAffordance("http://example.org/pickItem", "pickItem", "/pick", pickSchema);
    registerActionAffordance("http://example.org/orderMilk", "orderMilk", "/order");
  }

  @Override
  public Model getState() {
    ModelBuilder builder = new ModelBuilder();
    Resource artifactId = RDFS.rdf.createIRI(ConveyOntology.thisArtifact);
    int c = countFree();
    builder.add(artifactId, RDFS.rdf.createIRI(ConveyOntology.countFree), c);
    builder.add(artifactId, RDFS.rdf.createIRI(ConveyOntology.isWaiting), wait);
    for (int i=0; i<n;i++){
      for (int j=0;j<m;j++){
        if (isFree(i,j)){
          Resource freeId = RDFS.rdf.createBNode("item "+i+" "+j);
          builder.add(artifactId, RDFS.rdf.createIRI(ConveyOntology.hasFree), freeId);
          builder.add(freeId, RDFS.rdf.createIRI(ConveyOntology.hasX), i);
          builder.add(freeId, RDFS.rdf.createIRI(ConveyOntology.hasZ), j);
        }
      }
    }
    return builder.build();
  }
}
