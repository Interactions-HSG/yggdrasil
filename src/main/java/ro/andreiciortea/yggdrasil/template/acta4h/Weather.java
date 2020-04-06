package ro.andreiciortea.yggdrasil.template.acta4h;

import io.vertx.core.http.HttpMethod;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;

@Artifact(types = { "http://example.org/#OutsideWeatherArtifact" }, prefixes = {"xsd|http://www.w3.org/2001/XMLSchema#"}, additions = @RdfAddition(predicates ={"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}, objects = {"td:Thing"})
)
public class Weather {

  @ObservableProperty
  public String commonIdExt = "";

  @ObservableProperty
  public String conditionExt = "";

  @ObservableProperty
  public String conditionIdExt = "";

  @ObservableProperty
  public double humidityExt = 0;

  @ObservableProperty
  public String lastUpdateExt = "";

  @ObservableProperty
  public String observationTimeExt = "";

  @ObservableProperty
  public double pressureExt = 0;

  @ObservableProperty
  public String pressureTrendExt = "";

  @ObservableProperty
  public double rainExt = 0;

  @ObservableProperty
  public double snowExt = 0;

  @ObservableProperty
  public double tempFeelExt = 0;

  @ObservableProperty
  public double temperatureExt = 0;

  @ObservableProperty
  public String visibilityExt = "";

  @ObservableProperty
  public String windDirectionExt = "";

  @ObservableProperty
  public double windSpeedExt = 0;

  @Action(requestMethod = "PUT", path = "/commonIdExt", prefixes = {"xsd|http://www.w3.org/2001/XMLSchema#"}, inputs={"commonIdExt|xsd:string"})
  public String setCommonIdExt(String commonIdExt) {
    this.commonIdExt = commonIdExt;
    return this.commonIdExt;
  }

  @Action(requestMethod = "PUT", path = "/conditionExt", inputs={"conditionExt|xsd:string"})
  public String setConditionExt(String conditionExt) {
    this.conditionExt = conditionExt;
    return this.conditionExt;
  }

  @Action(requestMethod = "PUT", path = "/conditionIdExt", inputs={"conditionIdExt|xsd:string"})
  public String setConditionIdExt(String conditionIdExt) {
    this.conditionIdExt = conditionIdExt;
    return this.conditionIdExt;
  }

  @Action(requestMethod = "PUT", path = "/humidityExt", inputs={"humidityExt|xsd:double"})
  public double setHumidityExt(double humidityExt) {
    this.humidityExt = humidityExt;
    return this.humidityExt;
  }

  @Action(requestMethod = "PUT", path = "/lastUpdateExt", inputs={"lastUpdateExt|xsd:string"})
  public String setLastUpdateExt(String lastUpdateExt) {
    this.lastUpdateExt = lastUpdateExt;
    return this.lastUpdateExt;
  }

  @Action(requestMethod = "PUT", path = "/observationTimeExt", inputs={"observationTimeExt|xsd:string"})
  public String setObservationTimeExt(String observationTimeExt) {
    this.observationTimeExt = observationTimeExt;
    return this.observationTimeExt;
  }

  @Action(requestMethod = "PUT", path = "/pressureExt", inputs={"pressureExt|xsd:double"})
  public double setPressureExt(double pressureExt) {
    this.pressureExt = pressureExt;
    return this.pressureExt;
  }

  @Action(requestMethod = "PUT", path = "/pressureTrendExt", inputs={"pressureTrendExt|xsd:string"})
  public String setPressureTrendExt(String pressureTrendExt) {
    this.pressureTrendExt = pressureTrendExt;
    return this.pressureTrendExt;
  }

  @Action(requestMethod = "PUT", path = "/rainExt", inputs={"rainExt|xsd:double"})
  public double setRainExt(double rainExt) {
    this.rainExt = rainExt;
    return this.rainExt;
  }

  @Action(requestMethod = "PUT", path = "/snowExt", inputs={"snowExt|xsd:double"})
  public double setSnowExt(double snowExt) {
    this.snowExt = snowExt;
    return this.snowExt;
  }

  @Action(requestMethod = "PUT", path = "/tempFeelExt", inputs={"tempFeelExt|xsd:double"})
  public double setTempFeelExt(double tempFeelExt) {
    this.tempFeelExt = tempFeelExt;
    return this.tempFeelExt;
  }

  @Action(requestMethod = "PUT", path = "/temperatureExt", inputs={"temperatureExt|xsd:double"})
  public double setTemperatureExt(double temperatureExt) {
    this.temperatureExt = temperatureExt;
    return this.temperatureExt;
  }

  @Action(requestMethod = "PUT", path = "/visibilityExt", inputs={"visibilityExt|xsd:string"})
  public String setVisibilityExt(String visibilityExt) {
    this.visibilityExt = visibilityExt;
    return this.visibilityExt;
  }

  @Action(requestMethod = "PUT", path = "/windDirectionExt", inputs={"windDirectionExt|xsd:string"})
  public String setWindDirectionExt(String windDirectionExt) {
    this.windDirectionExt = windDirectionExt;
    return this.windDirectionExt;
  }

  @Action(requestMethod = "PUT", path = "/windSpeedExt", inputs={"windSpeedExt|xsd:double"})
  public double setWindSpeedExt(double windSpeedExt) {
    this.windSpeedExt = windSpeedExt;
    return this.windSpeedExt;
  }
}
