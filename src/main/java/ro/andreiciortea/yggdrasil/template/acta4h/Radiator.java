package ro.andreiciortea.yggdrasil.template.acta4h;

import io.vertx.core.http.HttpMethod;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;

@Artifact(type = "Radiator", additions =
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

  @Action(requestMethod = "PUT", path = "/command")
  public String setCommand(String command) {
    this.command = command;
    return this.command;
  }

  @Action(requestMethod = "PUT", path = "/instruction")
  public String setInstruction(String instruction) {
    this.instruction = instruction;
    return this.instruction;
  }

  @Action(requestMethod = "PUT", path = "/effectiveInstruction")
  public String setEffectiveInstruction(String effectiveInstruction) {
    this.effectiveInstruction = effectiveInstruction;
    return this.effectiveInstruction;
  }

  @Action(requestMethod = "PUT", path = "/instructionChange")
  public String setInstructionChange(String instructionChange) {
    this.instructionChange = instructionChange;
    return this.instructionChange;
  }

  @Action(requestMethod = "PUT", path = "/window")
  public String setWindwow(String window) {
    this.window = window;
    return this.window;
  }

  @Action(requestMethod = "PUT", path = "/baseMode")
  public String setBaseMode(String baseMode) {
    this.baseMode = baseMode;
    return this.baseMode;
  }

  @Action(requestMethod = "PUT", path = "/effectiveMode")
  public String setEffectiveMode(String effectiveMode) {
    this.effectiveMode = effectiveMode;
    return this.effectiveMode;
  }

  @Action(requestMethod = "PUT", path = "/presence")
  public String setPresence(String presence) {
    this.presence = presence;
    return this.presence;
  }

  @Action(requestMethod = "PUT", path = "/temperature")
  public double setTemperature(double temperature) {
    this.temperature = temperature;
    return this.temperature;
  }
}
