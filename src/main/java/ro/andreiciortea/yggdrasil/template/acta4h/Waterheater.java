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

  @RequestMapping(requestMethod = "PUT", path = "/Energy")
  public double setEnergy(double newEnergy) {
    energy = newEnergy;
    return energy;
  }

  @RequestMapping(requestMethod = "PUT", path = "/TotalEnergy")
  public double setTotalEnergy(double newEnergy) {
    totalEnergy = newEnergy;
    return totalEnergy;
  }

  @RequestMapping(requestMethod = "PUT", path = "/ElIntensity")
  public double setElIntensity(double newIntensity) {
    elIntensity = newIntensity;
    return elIntensity;
  }

  @RequestMapping(requestMethod = "PUT", path = "/Power")
  public double setPower(double newPower) {
    power = newPower;
    return power;
  }

  @RequestMapping(requestMethod = "PUT", path = "/Voltage")
  public double setVoltage(double newVoltage) {
    voltage = newVoltage;
    return voltage;
  }
}
