package ro.andreiciortea.yggdrasil.template.acta4h;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
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

  @Action(path = "/actions/setStatus")
  public String setStatus(String status) {
    this.status = status;
    return this.status;
  }

  @Action(path = "/actions/setOpen")
  public String setOpen(String open) {
    this.open = open;
    return this.open;
  }

  @Action(path = "/actions/setStop")
  public String setStop(String stop) {
    this.stop = stop;
    return this.stop;
  }
}
