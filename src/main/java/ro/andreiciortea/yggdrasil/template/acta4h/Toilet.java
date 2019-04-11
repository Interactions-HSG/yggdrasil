package ro.andreiciortea.yggdrasil.template.acta4h;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;

@Artifact(type = "Toilet", additions =
  @RdfAddition(predicates ={"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}, objects = {"td:Thing"})
)
public class Toilet {

  @ObservableProperty
  public double Eau_Froide_WC_Instantanee = 0;

  @ObservableProperty
  public double Eau_Froide_WC_Total = 0;

  @Action(path = "/actions/setEau_Froide_WC_Instantanee")
  public double setEau_Froide_WC_Instantanee(double eau_Froide_WC_Instantanee) {
    Eau_Froide_WC_Instantanee = eau_Froide_WC_Instantanee;
    return this.Eau_Froide_WC_Instantanee;
  }

  @Action(path = "/actions/setEau_Froide_WC_Total")
  public double setEau_Froide_WC_Total(double eau_Froide_WC_Total) {
    Eau_Froide_WC_Total = eau_Froide_WC_Total;
    return this.Eau_Froide_WC_Total;
  }
}
