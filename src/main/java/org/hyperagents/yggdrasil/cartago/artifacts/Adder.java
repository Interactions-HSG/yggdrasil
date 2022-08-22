package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.schemas.IntegerSchema;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifact;

public class Adder extends HypermediaArtifact {

  public void init(){

  }

  @OPERATION
  public void add(int x, int y, OpFeedbackParam<Integer> sumParam){
    System.out.println("adder performs add");
    sumParam.set(x+y);
    System.out.println("result in adder: "+sumParam.get());
  }

  @Override
  protected void registerInteractionAffordances() {
    DataSchema inputSchema = new ArraySchema.Builder()
      .addItem(new IntegerSchema.Builder().build())
      .addItem(new IntegerSchema.Builder().build())
        .build();
    registerActionAffordance("http://example.org/add", "add", "/add", inputSchema);
    registerFeedbackParameter("add");
  }
}
