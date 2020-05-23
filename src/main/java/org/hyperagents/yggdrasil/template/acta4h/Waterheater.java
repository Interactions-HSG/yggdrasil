package org.hyperagents.yggdrasil.template.acta4h;

import org.hyperagents.yggdrasil.template.annotation.Action;
import org.hyperagents.yggdrasil.template.annotation.Artifact;
import org.hyperagents.yggdrasil.template.annotation.ObservableProperty;
import org.hyperagents.yggdrasil.template.annotation.RdfAddition;

@Artifact(types = { "http://example.org/#Waterheater" }, prefixes = {"xsd|http://www.w3.org/2001/XMLSchema#"}, additions =
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

  @Action(requestMethod = "PUT", path = "/switchOnOff")
  public boolean switchOn() {
    onStatus = !onStatus;
    return onStatus;
  }

  @Action(requestMethod = "PUT", path = "/energy", inputs={"energy|xsd:double"})
  public double setEnergy(double energy) {
    this.energy = energy;
    return energy;
  }

  @Action(requestMethod = "PUT", path = "/totalEnergy", inputs={"totalEnergy|xsd:double"})
  public double setTotalEnergy(double totalEnergy) {
    this.totalEnergy = totalEnergy;
    return totalEnergy;
  }

  @Action(requestMethod = "PUT", path = "/elIntensity", inputs={"elIntensity|xsd:double"})
  public double setElIntensity(double elIntensity) {
    this.elIntensity = elIntensity;
    return elIntensity;
  }

  @Action(requestMethod = "PUT", path = "/power", inputs={"power|xsd:double"})
  public double setPower(double power) {
    this.power = power;
    return power;
  }

  @Action(requestMethod = "PUT", path = "/voltage", inputs={"voltage|xsd:double"})
  public double setVoltage(double voltage) {
    this.voltage = voltage;
    return voltage;
  }
}