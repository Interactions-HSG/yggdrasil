package ro.andreiciortea.yggdrasil.template.acta4h;

import io.vertx.core.http.HttpMethod;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;

@Artifact(type = "OutsideWeatherArtifact", additions =
  @RdfAddition(predicates ={"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}, objects = {"td:Thing"})
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

  @Action(requestMethod = "PUT", path = "/commonIdExt", inputs={"commonIdExt|www.test.type"})
  public String setCommonIdExt(String commonIdExt) {
    this.commonIdExt = commonIdExt;
    return this.commonIdExt;
  }

  @Action(requestMethod = "PUT", path = "/conditionExt")
  public String setConditionExt(String conditionExt) {
    this.conditionExt = conditionExt;
    return this.conditionExt;
  }

  @Action(requestMethod = "PUT", path = "/conditionIdExt")
  public String setConditionIdExt(String conditionIdExt) {
    this.conditionIdExt = conditionIdExt;
    return this.conditionIdExt;
  }

  @Action(requestMethod = "PUT", path = "/humidityExt")
  public double setHumidityExt(double humidityExt) {
    this.humidityExt = humidityExt;
    return this.humidityExt;
  }

  @Action(requestMethod = "PUT", path = "/lastUpdateExt")
  public String setLastUpdateExt(String lastUpdateExt) {
    this.lastUpdateExt = lastUpdateExt;
    return this.lastUpdateExt;
  }

  @Action(requestMethod = "PUT", path = "/observationTimeExt")
  public String setObservationTimeExt(String observationTimeExt) {
    this.observationTimeExt = observationTimeExt;
    return this.observationTimeExt;
  }

  @Action(requestMethod = "PUT", path = "/pressureExt")
  public double setPressureExt(double pressureExt) {
    this.pressureExt = pressureExt;
    return this.pressureExt;
  }

  @Action(requestMethod = "PUT", path = "/pressureTrendExt")
  public String setPressureTrendExt(String pressureTrendExt) {
    this.pressureTrendExt = pressureTrendExt;
    return this.pressureTrendExt;
  }

  @Action(requestMethod = "PUT", path = "/rainExt")
  public double setRainExt(double rainExt) {
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
