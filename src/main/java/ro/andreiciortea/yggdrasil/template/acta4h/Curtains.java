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
  public double partialEnergy = 0;

  @ObservableProperty
  public double totalEnergy = 0;

  @ObservableProperty
  public double intensity = 0;

  @ObservableProperty
  public double power = 0;

  @ObservableProperty
  public double tension = 0;

  @RequestMapping(requestMethod = "PUT", path = "/partialEnergy")
  public double setPartialEnergy(double partialEnergy) {
    this.partialEnergy = partialEnergy;
    return this.partialEnergy;
  }

  @RequestMapping(requestMethod = "PUT", path = "/totalEnergy")
  public double setTotalEnergy(double totalEnergy) {
    this.totalEnergy = totalEnergy;
    return this.totalEnergy;
  }

  @RequestMapping(requestMethod = "PUT", path = "/intensity")
  public double setIntensity(double intensity) {
    this.intensity = intensity;
    return this.intensity;
  }

  @RequestMapping(requestMethod = "PUT", path = "/power")
  public double setPower(double power) {
    this.power = power;
    return this.power;
  }

  @RequestMapping(requestMethod = "PUT", path = "/tension")
  public double setTension(double tension) {
    this.tension = tension;
    return this.tension;
  }
}
