package org.hyperagents.yggdrasil.template.acta4h;

import org.hyperagents.yggdrasil.template.annotation.Action;
import org.hyperagents.yggdrasil.template.annotation.Artifact;
import org.hyperagents.yggdrasil.template.annotation.ObservableProperty;
import org.hyperagents.yggdrasil.template.annotation.RdfAddition;

@Artifact(types = { "http://example.org/#Radiator" }, prefixes = {"xsd|http://www.w3.org/2001/XMLSchema#"}, additions =
  @RdfAddition(predicates ={"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}, objects = {"td:Thing"})
)
public class Radiator {

  @ObservableProperty
  public String command = "";

  @ObservableProperty
  public String instruction = "";

  @ObservableProperty
  public String effectiveInstruction = "";

  @ObservableProperty
  public String instructionChange = "";

  @ObservableProperty
  public String window = "";

  @ObservableProperty
  public String baseMode = "";

  @ObservableProperty
  public String effectiveMode = "";

  @ObservableProperty
  public String presence = "";

  @ObservableProperty
  public double temperature = 0;

  @Action(requestMethod = "PUT", path = "/command", inputs={"command|xsd:string"})
  public String setCommand(String command) {
    this.command = command;
    return this.command;
  }

  @Action(requestMethod = "PUT", path = "/instruction", inputs={"instruction|xsd:string"})
  public String setInstruction(String instruction) {
    this.instruction = instruction;
    return this.instruction;
  }

  @Action(requestMethod = "PUT", path = "/effectiveInstruction", inputs={"effectiveInstruction|xsd:string"})
  public String setEffectiveInstruction(String effectiveInstruction) {
    this.effectiveInstruction = effectiveInstruction;
    return this.effectiveInstruction;
  }

  @Action(requestMethod = "PUT", path = "/instructionChange", inputs={"instructionChange|xsd:string"})
  public String setInstructionChange(String instructionChange) {
    this.instructionChange = instructionChange;
    return this.instructionChange;
  }

  @Action(requestMethod = "PUT", path = "/window", inputs={"window|xsd:string"})
  public String setWindwow(String window) {
    this.window = window;
    return this.window;
  }

  @Action(requestMethod = "PUT", path = "/baseMode", inputs={"basMode|xsd:string"})
  public String setBaseMode(String baseMode) {
    this.baseMode = baseMode;
    return this.baseMode;
  }

  @Action(requestMethod = "PUT", path = "/effectiveMode", inputs={"effectiveMode|xsd:string"})
  public String setEffectiveMode(String effectiveMode) {
    this.effectiveMode = effectiveMode;
    return this.effectiveMode;
  }

  @Action(requestMethod = "PUT", path = "/presence", inputs={"presence|xsd:string"})
  public String setPresence(String presence) {
    this.presence = presence;
    return this.presence;
  }

  @Action(requestMethod = "PUT", path = "/temperature", inputs={"temperature|xsd:double"})
  public double setTemperature(double temperature) {
    this.temperature = temperature;
    return this.temperature;
  }
}
