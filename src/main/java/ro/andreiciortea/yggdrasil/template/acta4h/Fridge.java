package ro.andreiciortea.yggdrasil.template.acta4h;

import io.vertx.core.http.HttpMethod;

import ro.andreiciortea.yggdrasil.template.annotation.RequestMapping;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;

@Artifact(type = "Fridge", additions =
  @RdfAddition(predicates ={"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}, objects = {"td:Thing"})
)
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

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Energie_Partielle_Frigo")
  public double setEnergie_Partielle_Frigo(double energie_Partielle_Frigo) {
    this.energie_Partielle_Frigo = energie_Partielle_Frigo;
    return this.energie_Partielle_Frigo;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Energie_Totale_Frigo")
  public double setEnergie_Totale_Frigo(double energie_Totale_Frigo) {
    this.energie_Totale_Frigo = energie_Totale_Frigo;
    return this.energie_Totale_Frigo;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Intensite_Frigo")
  public double setIntensite_Frigo(double intensite_Frigo) {
    this.intensite_Frigo = intensite_Frigo;
    return this.intensite_Frigo;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Puissance_Frigo")
  public double setPuissance_Frigo(double puissance_Frigo) {
    this.puissance_Frigo = puissance_Frigo;
    return this.puissance_Frigo;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Tension_Frigo")
  public double setTension_Frigo(double tension_Frigo) {
    this.tension_Frigo = tension_Frigo;
    return this.tension_Frigo;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/C3")
  public String setC3(String c3) {
    this.c3 = c3;
    return this.c3;
  }
}
