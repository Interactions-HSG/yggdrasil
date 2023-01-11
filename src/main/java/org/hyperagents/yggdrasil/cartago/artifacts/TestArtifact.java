package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.ArtifactConfig;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.IntegerSchema;
import org.eclipse.rdf4j.query.algebra.In;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifact;

import java.util.ArrayList;
import java.util.List;

public class TestArtifact extends HypermediaArtifact {

  @OPERATION
  public void testOpFeedbackParam(int x, int y, OpFeedbackParam<List<Integer>> feedbackParam){
    List<Integer> list = new ArrayList<>();
    list.add(x);
    list.add(y);
    feedbackParam.set(list);
  }

  @Override
  protected void registerInteractionAffordances() {
    ArraySchema inputSchema = new ArraySchema.Builder()
      .addItem(new IntegerSchema.Builder().build())
      .addItem(new IntegerSchema.Builder().build())
      .build();

    registerActionAffordance("http://example.org/testOpFeedbackParam", "testOpFeedbackParam", "/testOpFeedbackParam", inputSchema);
    registerFeedbackParameter("testOpFeedbackParam");

  }
}
