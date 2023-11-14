package org.hyperagents.yggdrasil.cartago.entities;

import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;

public interface ActionDescription {
  String getActionName();

  String getActionClass();

  DataSchema getInputSchema();

  String getMethodName();

  String getRelativeUri();
}
