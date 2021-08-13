package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.schemas.IntegerSchema;
import org.hyperagents.yggdrasil.signifiers.SignifierHypermediaArtifact;

public class Adder extends SignifierHypermediaArtifact{

  public void init(){
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


  protected void registerInteractionAffordances(){
    registerSignifierAffordances();
    registerActionAffordance("http://example.org/nothing", "nothing", "/nothing");
    registerActionAffordance("http://example.org/get", "get", "/get");
    DataSchema input = new ArraySchema.Builder()
      .addItem(new IntegerSchema.Builder().build())
      .addItem(new IntegerSchema.Builder().build())
      .build();
    registerActionAffordance("http://example.org/addReturn","addReturn", "/addreturn", input);
    registerActionAffordance("http://example.org/add","add", "/add", input);
  }
}
