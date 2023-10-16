package org.hyperagents.yggdrasil.cartago;

import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;

public class ActionDescription {

  private final String actionName;

  private final String actionClass;

  private final String methodName;

  private final String relativeUri;

  private final DataSchema inputSchema;

  private ActionDescription(String actionName, String actionClass, String methodName, String relativeUri, DataSchema inputSchema){
    this.actionName = actionName;
    this.actionClass = actionClass;
    this.methodName = methodName;
    this.relativeUri = relativeUri;
    this.inputSchema = inputSchema;
  }

  public String getActionName() {
    return actionName;
  }

  public String getActionClass() {
    return actionClass;
  }

  public DataSchema getInputSchema() {
    return inputSchema;
  }

  public String getMethodName() {
    return methodName;
  }

  public String getRelativeUri() {
    return relativeUri;
  }

  public static class Builder {

    private final String actionName;

    private final String actionClass;

    private String methodName;

    private final String relativeUri;

    private DataSchema inputSchema;

    public Builder(String actionName, String actionClass, String relativeUri){
      this.actionName = actionName;
      this.actionClass = actionClass;
      this.relativeUri = relativeUri;
      this.methodName = "POST";
      this.inputSchema = null;
    }

    public Builder setMethodName(String methodName){
      this.methodName = methodName;
      return this;
    }

    public Builder setInputSchema(DataSchema inputSchema){
      this.inputSchema = inputSchema;
      return this;
    }

    public ActionDescription build(){
      return new ActionDescription(actionName, actionClass, methodName, relativeUri, inputSchema);
    }

  }
}
