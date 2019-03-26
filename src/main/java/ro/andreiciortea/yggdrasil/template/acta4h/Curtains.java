package ro.andreiciortea.yggdrasil.template.acta4h;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;

@Artifact(type = "Curtains")
public class Curtains {

  @ObservableProperty
  public double Energie_Partielle_Volets_Roulants = 0;

  @ObservableProperty
  public double Energie_Totale_Volets_Roulants = 0;

  @ObservableProperty
  public double Intensite_Volets_Roulants = 0;

  @ObservableProperty
  public double Puissance_Volets_Roulants = 0;

  @ObservableProperty
  public double Tension_Volets_Roulants = 0;

  @Action(path = "/actions/setEnergie_Partielle_Volets_Roulants")
  public double setEnergie_Partielle_Volets_Roulants(double energie_Partielle_Volets_Roulants) {
    Energie_Partielle_Volets_Roulants = energie_Partielle_Volets_Roulants;
    return this.Energie_Partielle_Volets_Roulants;
  }

  @Action(path = "/actions/setEnergie_Totale_Volets_Roulants")
  public double setEnergie_Totale_Volets_Roulants(double energie_Totale_Volets_Roulants) {
    Energie_Totale_Volets_Roulants = energie_Totale_Volets_Roulants;
    return this.Energie_Totale_Volets_Roulants;
  }

  @Action(path = "/actions/setIntensite_Volets_Roulants")
  public double setIntensite_Volets_Roulants(double intensite_Volets_Roulants) {
    Intensite_Volets_Roulants = intensite_Volets_Roulants;
    return this.Intensite_Volets_Roulants;
  }

  @Action(path = "/actions/setPuissance_Volets_Roulants")
  public double setPuissance_Volets_Roulants(double puissance_Volets_Roulants) {
    Puissance_Volets_Roulants = puissance_Volets_Roulants;
    return this.Puissance_Volets_Roulants;
  }

  @Action(path = "/actions/setTension_Volets_Roulants")
  public double setTension_Volets_Roulants(double tension_Volets_Roulants) {
    Tension_Volets_Roulants = tension_Volets_Roulants;
    return this.Tension_Volets_Roulants;
  }
}
