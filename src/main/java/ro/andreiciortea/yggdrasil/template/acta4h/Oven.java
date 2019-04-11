package ro.andreiciortea.yggdrasil.template.acta4h;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;

@Artifact(type = "Oven", additions =
  @RdfAddition(predicates ={"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}, objects = {"td:Thing"})
)
public class Oven {

  @ObservableProperty
  public double Energie_Partielle_Four = 0;

  @ObservableProperty
  public double Energie_Totale_Four = 0;

  @ObservableProperty
  public double Intensite_Four = 0;

  @ObservableProperty
  public double Puissance_Four = 0;

  @ObservableProperty
  public double Tension_Four = 0;


  @Action(path = "/actions/setEnergie_Partielle_Four")
  public double setEnergie_Partielle_Four(double energie_Partielle_Four) {
    Energie_Partielle_Four = energie_Partielle_Four;
    return this.Energie_Partielle_Four;
  }

  @Action(path = "/actions/setEnergie_Totale_Four")
  public double setEnergie_Totale_Four(double energie_Totale_Four) {
    Energie_Totale_Four = energie_Totale_Four;
    return this.Energie_Totale_Four;
  }

  @Action(path = "/actions/setIntensite_Four")
  public double setIntensite_Four(double intensite_Four) {
    Intensite_Four = intensite_Four;
    return this.Intensite_Four;
  }

  @Action(path = "/actions/setPuissance_Four")
  public double setPuissance_Four(double puissance_Four) {
    Puissance_Four = puissance_Four;
    return this.Puissance_Four;
  }

  @Action(path = "/actions/setTension_Four")
  public double setTension_Four(double tension_Four) {
    Tension_Four = tension_Four;
    return this.Tension_Four;
  }
}
