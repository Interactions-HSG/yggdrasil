package ro.andreiciortea.yggdrasil.template.acta4h;

import io.vertx.core.http.HttpMethod;

import ro.andreiciortea.yggdrasil.template.annotation.RequestMapping;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;

@Artifact(type = "OutsideWeatherArtifact", additions =
  @RdfAddition(predicates ={"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}, objects = {"td:Thing"})
)
public class Weather {

  @ObservableProperty
  public String commonId_ext = "";

  @ObservableProperty
  public String condition_ext = "";

  @ObservableProperty
  public String condition_ID_ext = "";

  @ObservableProperty
  public double humidity_ext = 0;

  @ObservableProperty
  public String lastUpdate_ext = "";

  @ObservableProperty
  public String observationTime_ext = "";

  @ObservableProperty
  public double pressure_ext = 0;

  @ObservableProperty
  public String pressure_Trend_ext = "";

  @ObservableProperty
  public double rain_ext = 0;

  @ObservableProperty
  public double snow_ext = 0;

  @ObservableProperty
  public double temp_Feel_ext = 0;

  @ObservableProperty
  public double temperature_ext = 0;

  @ObservableProperty
  public String visibility_ext = "";

  @ObservableProperty
  public String wind_Direction_ext = "";

  @ObservableProperty
  public double wind_Speed_ext = 0;

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/CommonId_ext")
  public String setCommonId_ext(String commonId_ext) {
    this.commonId_ext = commonId_ext;
    return this.commonId_ext;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Condition_ext")
  public String setCondition_ext(String condition_ext) {
    this.condition_ext = condition_ext;
    return this.condition_ext;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Condition_ID_ext")
  public String setCondition_ID_ext(String condition_ID_ext) {
    this.condition_ID_ext = condition_ID_ext;
    return this.condition_ID_ext;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Humidity_ext")
  public double setHumidity_ext(double humidity_ext) {
    this.humidity_ext = humidity_ext;
    return this.humidity_ext;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/LastUpdate_ext")
  public String setLastUpdate_ext(String lastUpdate_ext) {
    this.lastUpdate_ext = lastUpdate_ext;
    return this.lastUpdate_ext;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/ObservationTime_ext")
  public String setObservationTime_ext(String observationTime_ext) {
    this.observationTime_ext = observationTime_ext;
    return this.observationTime_ext;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Pressure_ext")
  public double setPressure_ext(double pressure_ext) {
    this.pressure_ext = pressure_ext;
    return this.pressure_ext;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Pressure_Trend_ext")
  public String setPressure_Trend_ext(String pressure_Trend_ext) {
    this.pressure_Trend_ext = pressure_Trend_ext;
    return this.pressure_Trend_ext;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Rain_ext")
  public double setRain_ext(double rain_ext) {
    this.rain_ext = rain_ext;
    return this.rain_ext;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Snow_ext")
  public double setSnow_ext(double snow_ext) {
    this.snow_ext = snow_ext;
    return this.snow_ext;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Temp_Feel_ext")
  public double setTemp_Feel_ext(double temp_Feel_ext) {
    this.temp_Feel_ext = temp_Feel_ext;
    return this.temp_Feel_ext;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Temperature_ext")
  public double setTemperature_ext(double temperature_ext) {
    this.temperature_ext = temperature_ext;
    return this.temperature_ext;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Visibility_ext")
  public String setVisibility_ext(String visibility_ext) {
    this.visibility_ext = visibility_ext;
    return this.visibility_ext;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Wind_Direction_ext")
  public String setWind_Direction_ext(String wind_Direction_ext) {
    this.wind_Direction_ext = wind_Direction_ext;
    return this.wind_Direction_ext;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Wind_Speed_ext")
  public double setWind_Speed_ext(double wind_Speed_ext) {
    this.wind_Speed_ext = wind_Speed_ext;
    return this.wind_Speed_ext;
  }
}
