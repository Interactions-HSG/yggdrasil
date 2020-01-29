package ro.andreiciortea.yggdrasil.template.acta4h;

import io.vertx.core.http.HttpMethod;

import ro.andreiciortea.yggdrasil.template.annotation.RequestMapping;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;

@Artifact(type = "TV", additions =
  @RdfAddition(predicates ={"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}, objects = {"td:Thing"})
)
public class TV {

  @ObservableProperty
  public String status = "OFF";

  @ObservableProperty
  public String open = "";

  @ObservableProperty
  public String stop = "";

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Status")
  public String setStatus(String status) {
    this.status = status;
    return this.status;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Open")
  public String setOpen(String open) {
    this.open = open;
    return this.open;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Stop")
  public String setStop(String stop) {
    this.stop = stop;
    return this.stop;
  }
}
