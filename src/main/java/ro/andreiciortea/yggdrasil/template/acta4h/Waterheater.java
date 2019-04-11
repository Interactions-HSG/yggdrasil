package ro.andreiciortea.yggdrasil.template.acta4h;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
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

  @Action(path = "/actions/switchOnOff")
  public boolean switchOn() {
    onStatus = !onStatus;
    return onStatus;
  }

  @Action(path = "/actions/setEnergy")
  public double setEnergy(double newEnergy) {
    energy = newEnergy;
    return energy;
  }

  @Action(path = "/actions/setTotalEnergy")
  public double setTotalEnergy(double newEnergy) {
    totalEnergy = newEnergy;
    return totalEnergy;
  }

  @Action(path = "/actions/setElIntensity")
  public double setElIntensity(double newIntensity) {
    elIntensity = newIntensity;
    return elIntensity;
  }

  @Action(path = "/actions/setPower")
  public double setPower(double newPower) {
    power = newPower;
    return power;
  }

  @Action(path = "/actions/setVoltage")
  public double setVoltage(double newVoltage) {
    voltage = newVoltage;
    return voltage;
  }
}
