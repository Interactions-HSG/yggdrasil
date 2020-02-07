package ro.andreiciortea.yggdrasil.template.acta4h;

import io.vertx.core.http.HttpMethod;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;

@Artifact(types = { "http://example.org/#Shower" }, additions =
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

  @Action(requestMethod = "PUT", path = "/instantHotWater")
  public double setTotalHotWater(double instantHotWater) {
    this.instantHotWater = instantHotWater;
    return this.instantHotWater;
  }

  @Action(requestMethod = "PUT", path = "/totalHotWater")
  public double setEau_Chaude_Douche_Total(double totalHotWater) {
    this.totalHotWater = totalHotWater;
    return this.totalHotWater;
  }

  @Action(requestMethod = "PUT", path = "/instantColdWater")
  public double setInstantColdWater(double instantColdWater) {
    this.instantColdWater = instantColdWater;
    return this.instantColdWater;
  }

  @Action(requestMethod = "PUT", path = "/totalColdWater")
  public double setTotalColdWater(double totalColdWater) {
    this.totalColdWater = totalColdWater;
    return this.totalColdWater;
  }

  @Action(requestMethod = "PUT", path = "/c21")
  public String setC21(String c21) {
    this.c21 = c21;
    return this.c21;
  }
}
