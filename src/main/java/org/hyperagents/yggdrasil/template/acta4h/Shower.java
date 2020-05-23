package org.hyperagents.yggdrasil.template.acta4h;

import org.hyperagents.yggdrasil.template.annotation.Action;
import org.hyperagents.yggdrasil.template.annotation.Artifact;
import org.hyperagents.yggdrasil.template.annotation.ObservableProperty;
import org.hyperagents.yggdrasil.template.annotation.RdfAddition;

@Artifact(types = { "http://example.org/#Shower" }, prefixes = {"xsd|http://www.w3.org/2001/XMLSchema#"}, additions =
  @RdfAddition(predicates ={"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}, objects = {"td:Thing"})
)
public class Shower {

  @ObservableProperty
  public double instantHotWater = 0;

  @ObservableProperty
  public double totalHotWater = 0;

  @ObservableProperty
  public double instantColdWater = 0;

  @ObservableProperty
  public double totalColdWater = 0;

  // Door open
  @ObservableProperty
  public String c21 = "";

  @Action(requestMethod = "PUT", path = "/instantHotWater", inputs={"instantHotWater|xsd:double"})
  public double setTotalHotWater(double instantHotWater) {
    this.instantHotWater = instantHotWater;
    return this.instantHotWater;
  }

  @Action(requestMethod = "PUT", path = "/totalHotWater", inputs={"totalHotWater|xsd:double"})
  public double setEau_Chaude_Douche_Total(double totalHotWater) {
    this.totalHotWater = totalHotWater;
    return this.totalHotWater;
  }

  @Action(requestMethod = "PUT", path = "/instantColdWater", inputs={"instantColdWater|xsd:double"})
  public double setInstantColdWater(double instantColdWater) {
    this.instantColdWater = instantColdWater;
    return this.instantColdWater;
  }

  @Action(requestMethod = "PUT", path = "/totalColdWater", inputs={"totalColdWater|xsd:double"})
  public double setTotalColdWater(double totalColdWater) {
    this.totalColdWater = totalColdWater;
    return this.totalColdWater;
  }

  @Action(requestMethod = "PUT", path = "/c21", inputs={"c21|xsd:string"})
  public String setC21(String c21) {
    this.c21 = c21;
    return this.c21;
  }
}
