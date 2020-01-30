package ro.andreiciortea.yggdrasil.template.acta4h;

import io.vertx.core.http.HttpMethod;

import ro.andreiciortea.yggdrasil.template.annotation.RequestMapping;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;

@Artifact(type = "Dishwasher", additions =
  @RdfAddition(predicates ={"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}, objects = {"td:Thing"})
)
public class Dishwasher {

  @ObservableProperty
  public double Energie_Partielle_Lave_Vaisselle = 0;

  @ObservableProperty
  public double Energie_Totale_Lave_Vaisselle = 0;

  @ObservableProperty
  public double Intensite_Lave_Vaisselle = 0;

  @ObservableProperty
  public double Puissance_Lave_Vaisselle = 0;

  @ObservableProperty
  public double Tension_Lave_Vaisselle = 0;

  @RequestMapping(requestMethod = "PUT", path = "/Energie_Partielle_Lave_Vaisselle")
  public double setEnergie_Partielle_Lave_Vaisselle(double energie_Partielle_Lave_Vaisselle) {
    Energie_Partielle_Lave_Vaisselle = energie_Partielle_Lave_Vaisselle;
    return this.Energie_Partielle_Lave_Vaisselle;
  }

  @RequestMapping(requestMethod = "PUT", path = "/Energie_Totale_Lave_Vaisselle")
  public double setEnergie_Totale_Lave_Vaisselle(double energie_Totale_Lave_Vaisselle) {
    Energie_Totale_Lave_Vaisselle = energie_Totale_Lave_Vaisselle;
    return this.Energie_Totale_Lave_Vaisselle;
  }

  @RequestMapping(requestMethod = "PUT", path = "/Intensite_Lave_Vaisselle")
  public double setIntensite_Lave_Vaisselle(double intensite_Lave_Vaisselle) {
    Intensite_Lave_Vaisselle = intensite_Lave_Vaisselle;
    return this.Intensite_Lave_Vaisselle;
  }

  @RequestMapping(requestMethod = "PUT", path = "/Puissance_Lave_Vaisselle")
  public double setPuissance_Lave_Vaisselle(double puissance_Lave_Vaisselle) {
    Puissance_Lave_Vaisselle = puissance_Lave_Vaisselle;
    return this.Puissance_Lave_Vaisselle;
  }

  @RequestMapping(requestMethod = "PUT", path = "/Tension_Lave_Vaisselle")
  public double setTension_Lave_Vaisselle(double tension_Lave_Vaisselle) {
    Tension_Lave_Vaisselle = tension_Lave_Vaisselle;
    return this.Tension_Lave_Vaisselle;
  }
}
