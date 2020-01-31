package ro.andreiciortea.yggdrasil.template.acta4h;

import io.vertx.core.http.HttpMethod;

import ro.andreiciortea.yggdrasil.template.annotation.RequestMapping;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;

@Artifact(type = "Waterheater", additions =
  @RdfAddition(predicates ={"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}, objects = {"td:Thing"})
)
public class Waterheater {

  @ObservableProperty
  public boolean onStatus = false;

  @ObservableProperty
  public double energy = 0;

  @ObservableProperty
  public double totalEnergy = 0;

  @ObservableProperty
  public double elIntensity = 0;

  @ObservableProperty
  public double power = 0;

  @ObservableProperty
  public double voltage = 0;

  @RequestMapping(requestMethod = "PUT", path = "/switchOnOff")
  public boolean switchOn() {
    onStatus = !onStatus;
    return onStatus;
  }

  @RequestMapping(requestMethod = "PUT", path = "/energy")
  public double setEnergy(double energy) {
    this.energy = energy;
    return energy;
  }

  @RequestMapping(requestMethod = "PUT", path = "/totalEnergy")
  public double setTotalEnergy(double totalEnergy) {
    this.totalEnergy = totalEnergy;
    return totalEnergy;
  }

  @RequestMapping(requestMethod = "PUT", path = "/elIntensity")
  public double setElIntensity(double elIntensity) {
    this.elIntensity = elIntensity;
    return elIntensity;
  }

  @RequestMapping(requestMethod = "PUT", path = "/power")
  public double setPower(double power) {
    this.power = power;
    return power;
  }

  @RequestMapping(requestMethod = "PUT", path = "/voltage")
  public double setVoltage(double voltage) {
    this.voltage = voltage;
    return voltage;
  }
}