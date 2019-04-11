package ro.andreiciortea.yggdrasil.template.acta4h;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;

@Artifact(type = "SinkBathroom", additions =
  @RdfAddition(predicates ={"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}, objects = {"td:Thing"})
)
public class SinkBathroom {

  @ObservableProperty
  public double eau_Chaude_Lavabo_Instantanee = 0;

  @ObservableProperty
  public double eau_Chaude_Lavabo_Total = 0;

  @ObservableProperty
  public double eau_Froide_Lavabo_Instantanee = 0;

  @ObservableProperty
  public double eau_Froide_Lavabo_Total = 0;

  @Action(path = "/actions/setEau_Chaude_Lavabo_Instantanee")
  public double setEau_Chaude_Lavabo_Instantanee(double eau_Chaude_Lavabo_Instantanee) {
    this.eau_Chaude_Lavabo_Instantanee = eau_Chaude_Lavabo_Instantanee;
    return this.eau_Chaude_Lavabo_Instantanee;
  }

  @Action(path = "/actions/setEau_Chaude_Lavabo_Total")
  public double setEau_Chaude_Lavabo_Total(double eau_Chaude_Lavabo_Total) {
    this.eau_Chaude_Lavabo_Total = eau_Chaude_Lavabo_Total;
    return this.eau_Chaude_Lavabo_Total;
  }

  @Action(path = "/actions/setEau_Froide_Lavabo_Instantanee")
  public double setEau_Froide_Lavabo_Instantanee(double eau_Froide_Lavabo_Instantanee) {
    this.eau_Froide_Lavabo_Instantanee = eau_Froide_Lavabo_Instantanee;
    return this.eau_Froide_Lavabo_Instantanee;
  }

  @Action(path = "/actions/setEau_Froide_Lavabo_Total")
  public double setEau_Froide_Lavabo_Total(double eau_Froide_Lavabo_Total) {
    this.eau_Froide_Lavabo_Total = eau_Froide_Lavabo_Total;
    return this.eau_Froide_Lavabo_Total;
  }
}
