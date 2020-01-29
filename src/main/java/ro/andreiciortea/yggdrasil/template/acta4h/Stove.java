package ro.andreiciortea.yggdrasil.template.acta4h;

import io.vertx.core.http.HttpMethod;

import ro.andreiciortea.yggdrasil.template.annotation.RequestMapping;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;

@Artifact(type = "Stove", additions =
  @RdfAddition(predicates ={"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}, objects = {"td:Thing"})
)
public class Stove {

  @ObservableProperty
  public double Energie_Partielle_Plaques = 0;

  @ObservableProperty
  public double Energie_Totale_Plaques = 0;

  @ObservableProperty
  public double Intensite_Plaques = 0;

  @ObservableProperty
  public double Puissance_Plaques = 0;

  @ObservableProperty
  public double Tension_Plaques = 0;

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Energie_Partielle_Plaques")
  public double setEnergie_Partielle_Plaques(double energie_Partielle_Plaques) {
    Energie_Partielle_Plaques = energie_Partielle_Plaques;
    return this.Energie_Partielle_Plaques;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Energie_Totale_Plaques")
  public double setEnergie_Totale_Plaques(double energie_Totale_Plaques) {
    Energie_Totale_Plaques = energie_Totale_Plaques;
    return this.Energie_Totale_Plaques;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Intensite_Plaques")
  public double setIntensite_Plaques(double intensite_Plaques) {
    Intensite_Plaques = intensite_Plaques;
    return this.Intensite_Plaques;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Puissance_Plaques")
  public double setPuissance_Plaques(double puissance_Plaques) {
    Puissance_Plaques = puissance_Plaques;
    return this.Puissance_Plaques;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Tension_Plaques")
  public double setTension_Plaques(double tension_Plaques) {
    Tension_Plaques = tension_Plaques;
    return this.Tension_Plaques;
  }
}
