package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.IntegerSchema;
import ch.unisg.ics.interactions.wot.td.schemas.NumberSchema;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifact;

import java.util.ArrayList;
import java.util.List;

public class IntellIoTArtifact extends HypermediaArtifact {

  @OPERATION
  public void normalizeValues(double alpha, double x, double y, OpFeedbackParam<List<Double>> newValues){
    double x1 = x/1000;
    double y1 = y/1000;
    double newAlpha = normalize_boundaries(alpha, -20, 25);
    double newX = normalize_boundaries(x1, 0.08, 1.05);
    double newY = normalize_boundaries(y1, 0.365, 0.5);
    List<Double> values = new ArrayList<>();
    values.add(newAlpha);
    values.add(newX);
    values.add(newY);
    newValues.set(values);

  }

  public double normalize_boundaries(double x, double low, double high){
    double newX = 0;
    if (x < low){
      newX = low;
    } else {
      if (x > high){
        newX = high;
      } else {
        newX = x;
      }
    }
    return newX;
  }

  @OPERATION
  public void generateRandomId(int c, OpFeedbackParam<Integer> p){
    double r = Math.random();
    double m = c * r;
    int n = (int) Math.floor(m);
    p.set(n);


  }


  protected void registerInteractionAffordances() {
    ArraySchema normalizeSchema = new ArraySchema.Builder()
      .addItem(new NumberSchema.Builder().build())
      .addItem(new NumberSchema.Builder().build())
      .addItem(new NumberSchema.Builder().build())
      .build();
    registerActionAffordance("normalizeValues", "normalizeValues", "/normalizeValues", normalizeSchema);
    registerFeedbackParameter("normalizeValues");
    ArraySchema randomSchema = new ArraySchema.Builder()
      .addItem(new IntegerSchema.Builder().build())
      .build();
    registerActionAffordance("generateRandomId", "generateRandomId", "/generateRandomId", randomSchema);
    registerFeedbackParameter("generateRandomId");

  }
}
