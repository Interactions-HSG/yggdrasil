package ro.andreiciortea.yggdrasil.template.acta4h;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;

@Artifact(type = "Outside Weather Artifact", additions =
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

  @Action(path = "/actions/setCommonId_ext")
  public String setCommonId_ext(String commonId_ext) {
    this.commonId_ext = commonId_ext;
    return this.commonId_ext;
  }

  @Action(path = "/actions/setCondition_ext")
  public String setCondition_ext(String condition_ext) {
    this.condition_ext = condition_ext;
    return this.condition_ext;
  }

  @Action(path = "/actions/setCondition_ID_ext")
  public String setCondition_ID_ext(String condition_ID_ext) {
    this.condition_ID_ext = condition_ID_ext;
    return this.condition_ID_ext;
  }

  @Action(path = "/actions/setHumidity_ext")
  public double setHumidity_ext(double humidity_ext) {
    this.humidity_ext = humidity_ext;
    return this.humidity_ext;
  }

  @Action(path = "/actions/setLastUpdate_ext")
  public String setLastUpdate_ext(String lastUpdate_ext) {
    this.lastUpdate_ext = lastUpdate_ext;
    return this.lastUpdate_ext;
  }

  @Action(path = "/actions/setObservationTime_ext")
  public String setObservationTime_ext(String observationTime_ext) {
    this.observationTime_ext = observationTime_ext;
    return this.observationTime_ext;
  }

  @Action(path = "/actions/setPressure_ext")
  public double setPressure_ext(double pressure_ext) {
    this.pressure_ext = pressure_ext;
    return this.pressure_ext;
  }

  @Action(path = "/actions/setPressure_Trend_ext")
  public String setPressure_Trend_ext(String pressure_Trend_ext) {
    this.pressure_Trend_ext = pressure_Trend_ext;
    return this.pressure_Trend_ext;
  }

  @Action(path = "/actions/setRain_ext")
  public double setRain_ext(double rain_ext) {
    this.rain_ext = rain_ext;
    return this.rain_ext;
  }

  @Action(path = "/actions/setSnow_ext")
  public double setSnow_ext(double snow_ext) {
    this.snow_ext = snow_ext;
    return this.snow_ext;
  }

  @Action(path = "/actions/setTemp_Feel_ext")
  public double setTemp_Feel_ext(double temp_Feel_ext) {
    this.temp_Feel_ext = temp_Feel_ext;
    return this.temp_Feel_ext;
  }

  @Action(path = "/actions/setTemperature_ext")
  public double setTemperature_ext(double temperature_ext) {
    this.temperature_ext = temperature_ext;
    return this.temperature_ext;
  }

  @Action(path = "/actions/setVisibility_ext")
  public String setVisibility_ext(String visibility_ext) {
    this.visibility_ext = visibility_ext;
    return this.visibility_ext;
  }

  @Action(path = "/actions/setWind_Direction_ext")
  public String setWind_Direction_ext(String wind_Direction_ext) {
    this.wind_Direction_ext = wind_Direction_ext;
    return this.wind_Direction_ext;
  }

  @Action(path = "/actions/setWind_Speed_ext")
  public double setWind_Speed_ext(double wind_Speed_ext) {
    this.wind_Speed_ext = wind_Speed_ext;
    return this.wind_Speed_ext;
  }
}
