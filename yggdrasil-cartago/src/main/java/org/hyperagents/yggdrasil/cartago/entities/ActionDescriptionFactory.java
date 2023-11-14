package org.hyperagents.yggdrasil.cartago.entities;

import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;

public class ActionDescriptionFactory {
  private final String actionName;
  private final String actionClass;
  private final String relativeUri;
  private String methodName;
  private DataSchema inputSchema;

  public ActionDescriptionFactory(
      final String actionName,
      final String actionClass,
      final String relativeUri
  ) {
    this.actionName = actionName;
    this.actionClass = actionClass;
    this.relativeUri = relativeUri;
    this.methodName = "POST";
    this.inputSchema = null;
  }

  public ActionDescriptionFactory setMethodName(final String methodName) {
    this.methodName = methodName;
    return this;
  }

  public ActionDescriptionFactory setInputSchema(final DataSchema inputSchema) {
    this.inputSchema = inputSchema;
    return this;
  }

  public ActionDescription build() {
    return new ActionDescriptionImpl(
      this.actionName,
      this.actionClass,
      this.methodName,
      this.relativeUri,
      this.inputSchema
    );
  }

  private record ActionDescriptionImpl(
      String actionName,
      String actionClass,
      String methodName,
      String relativeUri,
      DataSchema inputSchema
  ) implements ActionDescription {

    @Override
    public String getActionName() {
      return this.actionName;
    }

    @Override
    public String getActionClass() {
      return this.actionClass;
    }

    @Override
    public DataSchema getInputSchema() {
      return this.inputSchema;
    }

    @Override
    public String getMethodName() {
      return this.methodName;
    }

    @Override
    public String getRelativeUri() {
      return this.relativeUri;
    }
  }
}
