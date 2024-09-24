package org.hyperagents.yggdrasil.model;

import io.vertx.core.shareddata.Shareable;
import java.util.List;

public interface Agents extends Shareable {
  List<YggdrasilAgent> getAgents();

}