package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.schemas.IntegerSchema;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.hyperagents.yggdrasil.signifiers.SignifierHypermediaArtifact;

public class Adder extends SignifierHypermediaArtifact{

  public void init(){
  }

  @Override
  public Model getState() {
    return new ModelBuilder().build();
  }

  @OPERATION
  public void nothing(){
    System.out.println("nothing");
  }

  @OPERATION
  public void get(OpFeedbackParam<Object> returnParam){
    returnParam.set(0);
  }

  @OPERATION
  public void addReturn(int n, int m, OpFeedbackParam<Object> returnParam){
    Integer integer = n + m;
    returnParam.set(integer);
  }

  @OPERATION
  public void add(int n, int m){
    int result = n + m;
    System.out.println(result);
  }

  @OPERATION
  public void add2(int n){
    int result = n + 2;
    System.out.println(result);
  }


  protected void registerInteractionAffordances(){
    registerSignifierAffordances();
    registerActionAffordance("http://example.org/nothing", "nothing", "/nothing");
    registerActionAffordance("http://example.org/get", "get", "/get");
    DataSchema input = new ArraySchema.Builder()
      .addItem(new IntegerSchema.Builder().build())
      .addItem(new IntegerSchema.Builder().build())
      .build();
    DataSchema inputAdd2 = new ArraySchema.Builder()
      .addItem(new IntegerSchema.Builder().build())
      .build();
    registerActionAffordance("http://example.org/addReturn","addReturn", "/addreturn", input);
    registerActionAffordance("http://example.org/add","add", "/add", input);
    DataSchema input2 = new IntegerSchema.Builder().build();
    registerActionAffordance("http://example.org/add2","add2", "/add2", inputAdd2);
  }
}
