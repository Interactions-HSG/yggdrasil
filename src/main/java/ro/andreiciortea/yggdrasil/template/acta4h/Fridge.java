package ro.andreiciortea.yggdrasil.template.acta4h;

import io.vertx.core.http.HttpMethod;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;

@Artifact(type = "http://example.org/#Fridge", additions =
  @RdfAddition(predicates ={"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}, objects = {"td:Thing"})
)
public class Fridge {

  @ObservableProperty
  public double partialEnergy = 0;

  @ObservableProperty
  public double totalEnergy = 0;

  @ObservableProperty
  public double intensity = 0;

  @ObservableProperty
  public double power = 0;

  @ObservableProperty
  public double tension = 0;

  @ObservableProperty
  public String c3 = "";

  @Action(requestMethod = "PUT", path = "/partialEnergy")
  public double setPartialEnergy(double partialEnergy) {
    this.partialEnergy = partialEnergy;
    return this.partialEnergy;
  }

  @Action(requestMethod = "PUT", path = "/totalEnergy")
  public double setTotalEnergy(double totalEnergy) {
    this.totalEnergy = totalEnergy;
    return this.totalEnergy;
  }

  @Action(requestMethod = "PUT", path = "/intensity")
  public double setIntensity(double intensity) {
    this.intensity = intensity;
    return this.intensity;
  }

  @Action(requestMethod = "PUT", path = "/power")
  public double setPower(double power) {
    this.power = power;
    return this.power;
  }

  @Action(requestMethod = "PUT", path = "/tension")
  public double setTension(double tension) {
    this.tension = tension;
    return this.tension;
  }

  @Action(requestMethod = "PUT", path = "/c3")
  public String setC3(String c3) {
    this.c3 = c3;
    return this.c3;
  }

}
