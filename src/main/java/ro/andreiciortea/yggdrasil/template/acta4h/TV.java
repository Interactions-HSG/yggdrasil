package ro.andreiciortea.yggdrasil.template.acta4h;

import io.vertx.core.http.HttpMethod;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;

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
