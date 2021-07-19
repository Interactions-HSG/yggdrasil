package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import cartago.OpFeedbackParam;

public class Adder {

  @OPERATION
  public void add(int n, int m, OpFeedbackParam<Object> returnParam){
    Integer integer = n + m;
    returnParam.set(integer);
  }
}
