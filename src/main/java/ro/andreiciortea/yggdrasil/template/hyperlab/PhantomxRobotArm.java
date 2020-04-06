package ro.andreiciortea.yggdrasil.template.hyperlab;

import io.vertx.core.http.HttpMethod;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;

@Artifact(types = { "td:Thing", "eve:Artifact" },
          prefixes = {
            "td|http://www.w3.org/ns/td#",
            "xsd|http://www.w3.org/2001/XMLSchema#",
            "iot|http://iotschema.org/",
            "http|http://iotschema.org/protocol/http",
            "eve|http://w3id.org/eve#",
            "ex|http://example.com/"
          },
          additions = @RdfAddition(predicates ={"td:name", "td:base"}, objects = {"Robot3", "http://192.168.2.50/"})
)
public class PhantomxRobotArm {

// TODO: support full IRI vs. forward request to robot
  @Action(requestMethod = "PUT", path = "https://api.interactions.ics.unisg.ch/leubot/wrist/rotation")
  public void rotate(double rainExt) {
    this.rainExt = rainExt;
    return this.rainExt;
  }

  @Action(requestMethod = "PUT", path = "/snowExt")
  public double setSnowExt(double snowExt) {
    this.snowExt = snowExt;
    return this.snowExt;
  }

  @Action(requestMethod = "PUT", path = "/tempFeelExt")
  public double setTempFeelExt(double tempFeelExt) {
    this.tempFeelExt = tempFeelExt;
    return this.tempFeelExt;
  }

  @Action(requestMethod = "PUT", path = "/temperatureExt")
  public double setTemperatureExt(double temperatureExt) {
    this.temperatureExt = temperatureExt;
    return this.temperatureExt;
  }

  @Action(requestMethod = "PUT", path = "/visibilityExt")
  public String setVisibilityExt(String visibilityExt) {
    this.visibilityExt = visibilityExt;
    return this.visibilityExt;
  }

  @Action(requestMethod = "PUT", path = "/windDirectionExt")
  public String setWindDirectionExt(String windDirectionExt) {
    this.windDirectionExt = windDirectionExt;
    return this.windDirectionExt;
  }

  @Action(requestMethod = "PUT", path = "/windSpeedExt")
  public double setWindSpeedExt(double windSpeedExt) {
    this.windSpeedExt = windSpeedExt;
    return this.windSpeedExt;
  }
}
