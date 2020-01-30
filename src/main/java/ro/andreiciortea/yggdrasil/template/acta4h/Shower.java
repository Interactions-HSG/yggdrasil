package ro.andreiciortea.yggdrasil.template.acta4h;

import io.vertx.core.http.HttpMethod;

import ro.andreiciortea.yggdrasil.template.annotation.RequestMapping;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;

@Artifact(type = "Shower", additions =
  @RdfAddition(predicates ={"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}, objects = {"td:Thing"})
)
public class Shower {

  @ObservableProperty
  public double eau_Chaude_Douche_Instantanee = 0;

  @ObservableProperty
  public double eau_Chaude_Douche_Total = 0;

  @ObservableProperty
  public double eau_Froide_Douche_Instantanee = 0;

  @ObservableProperty
  public double eau_Froide_Douche_Total = 0;

  // Door open
  @ObservableProperty
  public String c21 = "";

  @RequestMapping(requestMethod = "PUT", path = "/Eau_Chaude_Douche_Instantanee")
  public double setEau_Chaude_Douche_Instantanee(double eau_Chaude_Douche_Instantanee) {
    this.eau_Chaude_Douche_Instantanee = eau_Chaude_Douche_Instantanee;
    return this.eau_Chaude_Douche_Instantanee;
  }

  @RequestMapping(requestMethod = "PUT", path = "/Eau_Chaude_Douche_Total")
  public double setEau_Chaude_Douche_Total(double eau_Chaude_Douche_Total) {
    this.eau_Chaude_Douche_Total = eau_Chaude_Douche_Total;
    return this.eau_Chaude_Douche_Total;
  }

  @RequestMapping(requestMethod = "PUT", path = "/Eau_Froide_Douche_Instantanee")
  public double setEau_Froide_Douche_Instantanee(double eau_Froide_Douche_Instantanee) {
    this.eau_Froide_Douche_Instantanee = eau_Froide_Douche_Instantanee;
    return this.eau_Froide_Douche_Instantanee;
  }

  @RequestMapping(requestMethod = "PUT", path = "/Eau_Froide_Douche_Total")
  public double setEau_Froide_Douche_Total(double eau_Froide_Douche_Total) {
    this.eau_Froide_Douche_Total = eau_Froide_Douche_Total;
    return this.eau_Froide_Douche_Total;
  }

  @RequestMapping(requestMethod = "PUT", path = "/C21")
  public String setC21(String c21) {
    this.c21 = c21;
    return this.c21;
  }
}
