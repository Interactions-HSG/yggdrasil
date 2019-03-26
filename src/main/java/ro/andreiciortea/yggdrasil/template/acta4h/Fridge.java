package ro.andreiciortea.yggdrasil.template.acta4h;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;

@Artifact(type = "Fridge")
public class Fridge {

  @ObservableProperty
  public double energie_Partielle_Frigo = 0;

  @ObservableProperty
  public double energie_Totale_Frigo = 0;

  @ObservableProperty
  public double intensite_Frigo = 0;

  @ObservableProperty
  public double puissance_Frigo = 0;

  @ObservableProperty
  public double tension_Frigo = 0;

  @ObservableProperty
  public String c3 = "";

  @Action(path = "/actions/setEnergie_Partielle_Frigo")
  public double setEnergie_Partielle_Frigo(double energie_Partielle_Frigo) {
    this.energie_Partielle_Frigo = energie_Partielle_Frigo;
    return this.energie_Partielle_Frigo;
  }

  @Action(path = "/actions/setEnergie_Totale_Frigo")
  public double setEnergie_Totale_Frigo(double energie_Totale_Frigo) {
    this.energie_Totale_Frigo = energie_Totale_Frigo;
    return this.energie_Totale_Frigo;
  }

  @Action(path = "/actions/setIntensite_Frigo")
  public double setIntensite_Frigo(double intensite_Frigo) {
    this.intensite_Frigo = intensite_Frigo;
    return this.intensite_Frigo;
  }

  @Action(path = "/actions/setPuissance_Frigo")
  public double setPuissance_Frigo(double puissance_Frigo) {
    this.puissance_Frigo = puissance_Frigo;
    return this.puissance_Frigo;
  }

  @Action(path = "/actions/setTension_Frigo")
  public double setTension_Frigo(double tension_Frigo) {
    this.tension_Frigo = tension_Frigo;
    return this.tension_Frigo;
  }

  @Action(path = "/actions/setC3")
  public String setC3(String c3) {
    this.c3 = c3;
    return this.c3;
  }
}
