package ro.andreiciortea.yggdrasil.template.acta4h;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;

@Artifact(type = "Washingmachine")
public class Washingmachine {

  @ObservableProperty
  public double Energie_Partielle_Lave_Linge = 0;

  @ObservableProperty
  public double Energie_Totale_Lave_Linge = 0;

  @ObservableProperty
  public double Intensite_Lave_Linge = 0;

  @ObservableProperty
  public double Puissance_Lave_Linge = 0;

  @ObservableProperty
  public double Tension_Lave_Linge = 0;

  @Action(path = "/actions/setEnergie_Partielle_Lave_Linge")
  public double setEnergie_Partielle_Lave_Linge(double energie_Partielle_Lave_Linge) {
    Energie_Partielle_Lave_Linge = energie_Partielle_Lave_Linge;
    return this.Energie_Partielle_Lave_Linge;
  }

  @Action(path = "/actions/setEnergie_Totale_Lave_Linge")
  public double setEnergie_Totale_Lave_Linge(double energie_Totale_Lave_Linge) {
    Energie_Totale_Lave_Linge = energie_Totale_Lave_Linge;
    return this.Energie_Totale_Lave_Linge;
  }

  @Action(path = "/actions/setIntensite_Lave_Linge")
  public double setIntensite_Lave_Linge(double intensite_Lave_Linge) {
    Intensite_Lave_Linge = intensite_Lave_Linge;
    return this.Intensite_Lave_Linge;
  }

  @Action(path = "/actions/setPuissance_Lave_Linge")
  public double setPuissance_Lave_Linge(double puissance_Lave_Linge) {
    Puissance_Lave_Linge = puissance_Lave_Linge;
    return this.Puissance_Lave_Linge;
  }

  @Action(path = "/actions/setTension_Lave_Linge")
  public double setTension_Lave_Linge(double tension_Lave_Linge) {
    Tension_Lave_Linge = tension_Lave_Linge;
    return this.Tension_Lave_Linge;
  }
}
