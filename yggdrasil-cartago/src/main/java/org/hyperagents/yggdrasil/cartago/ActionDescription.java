package org.hyperagents.yggdrasil.cartago;

import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;

public final class ActionDescription {
  private final String actionName;
  private final String actionClass;
  private final String methodName;
  private final String relativeUri;
  private final DataSchema inputSchema;

  private ActionDescription(
    final String actionName,
    final String actionClass,
    final String methodName,
    final String relativeUri,
    final DataSchema inputSchema
  ) {
    this.actionName = actionName;
    this.actionClass = actionClass;
    this.methodName = methodName;
    this.relativeUri = relativeUri;
    this.inputSchema = inputSchema;
  }

  public String getActionName() {
    return this.actionName;
  }

  public String getActionClass() {
    return this.actionClass;
  }

  public DataSchema getInputSchema() {
    return this.inputSchema;
  }

  public String getMethodName() {
    return this.methodName;
  }

  public String getRelativeUri() {
    return this.relativeUri;
  }

  public static class Builder {
    private final String actionName;
    private final String actionClass;
    private final String relativeUri;
    private String methodName;
    private DataSchema inputSchema;

    public Builder(final String actionName, final String actionClass, final String relativeUri) {
      this.actionName = actionName;
      this.actionClass = actionClass;
      this.relativeUri = relativeUri;
      this.methodName = "POST";
      this.inputSchema = null;
    }

    public Builder setMethodName(final String methodName) {
      this.methodName = methodName;
      return this;
    }

    public Builder setInputSchema(final DataSchema inputSchema) {
      this.inputSchema = inputSchema;
      return this;
    }
    public ActionDescription build() {
      return new ActionDescription(
        this.actionName,
        this.actionClass,
        this.methodName,
        this.relativeUri,
        this.inputSchema
      );
    }
  }
}
