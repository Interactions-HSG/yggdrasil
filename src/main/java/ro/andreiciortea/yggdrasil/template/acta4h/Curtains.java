package ro.andreiciortea.yggdrasil.template.acta4h;

import io.vertx.core.http.HttpMethod;

import ro.andreiciortea.yggdrasil.template.annotation.RequestMapping;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;


@Artifact(type = "Curtains", additions =
  @RdfAddition(predicates ={"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}, objects = {"td:Thing"})
)
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

  @RequestMapping(requestMethod = "PUT", path = "/Energie_Partielle_Volets_Roulants")
  public double setEnergie_Partielle_Volets_Roulants(double energie_Partielle_Volets_Roulants) {
    Energie_Partielle_Volets_Roulants = energie_Partielle_Volets_Roulants;
    return this.Energie_Partielle_Volets_Roulants;
  }

  @RequestMapping(requestMethod = "PUT", path = "/Energie_Totale_Volets_Roulants")
  public double setEnergie_Totale_Volets_Roulants(double energie_Totale_Volets_Roulants) {
    Energie_Totale_Volets_Roulants = energie_Totale_Volets_Roulants;
    return this.Energie_Totale_Volets_Roulants;
  }

  @RequestMapping(requestMethod = "PUT", path = "/Intensite_Volets_Roulants")
  public double setIntensite_Volets_Roulants(double intensite_Volets_Roulants) {
    Intensite_Volets_Roulants = intensite_Volets_Roulants;
    return this.Intensite_Volets_Roulants;
  }

  @RequestMapping(requestMethod = "PUT", path = "/Puissance_Volets_Roulants")
  public double setPuissance_Volets_Roulants(double puissance_Volets_Roulants) {
    Puissance_Volets_Roulants = puissance_Volets_Roulants;
    return this.Puissance_Volets_Roulants;
  }

  @RequestMapping(requestMethod = "PUT", path = "/Tension_Volets_Roulants")
  public double setTension_Volets_Roulants(double tension_Volets_Roulants) {
    Tension_Volets_Roulants = tension_Volets_Roulants;
    return this.Tension_Volets_Roulants;
  }
}
