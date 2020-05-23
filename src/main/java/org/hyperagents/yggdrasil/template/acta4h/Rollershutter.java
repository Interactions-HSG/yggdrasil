package org.hyperagents.yggdrasil.template.acta4h;

import org.hyperagents.yggdrasil.template.annotation.Action;
import org.hyperagents.yggdrasil.template.annotation.Artifact;
import org.hyperagents.yggdrasil.template.annotation.ObservableProperty;
import org.hyperagents.yggdrasil.template.annotation.RdfAddition;

@Artifact(types = { "http://example.org/#Rollershutter" }, prefixes = {"xsd|http://www.w3.org/2001/XMLSchema#"}, additions =
  @RdfAddition(predicates ={"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}, objects = {"td:Thing"})
)
public class Rollershutter {

  @ObservableProperty
  public double percentage = 0.0;

  @Action(requestMethod = "PUT", path = "/percentage", inputs={"percentage|xsd:double"})
  public double setPercentage(double percentage) {
    this.percentage = percentage;
    return this.percentage;
  }
}
