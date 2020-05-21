package org.hyperagents.yggdrasil.template.acta4h;

import org.hyperagents.yggdrasil.template.annotation.Action;
import org.hyperagents.yggdrasil.template.annotation.Artifact;
import org.hyperagents.yggdrasil.template.annotation.ObservableProperty;
import org.hyperagents.yggdrasil.template.annotation.RdfAddition;

@Artifact(types = { "http://example.org/#TV" }, prefixes = {"xsd|http://www.w3.org/2001/XMLSchema#"}, additions =
  @RdfAddition(predicates ={"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}, objects = {"td:Thing"})
)
public class TV {

  @ObservableProperty
  public String status = "OFF";

  @ObservableProperty
  public String open = "";

  @ObservableProperty
  public String stop = "";

  @Action(requestMethod = "PUT", path = "/status", inputs={"status|xsd:string"})
  public String setStatus(String status) {
    this.status = status;
    return this.status;
  }

  @Action(requestMethod = "PUT", path = "/open", inputs={"open|xsd:string"})
  public String setOpen(String open) {
    this.open = open;
    return this.open;
  }

  @Action(requestMethod = "PUT", path = "/stop", inputs={"stop|xsd:string"})
  public String setStop(String stop) {
    this.stop = stop;
    return this.stop;
  }
}
