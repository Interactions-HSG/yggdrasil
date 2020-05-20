package ro.andreiciortea.yggdrasil.template.acta4h;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;

@Artifact(types = { "http://example.org/#MusicPlayer" }, prefixes = {"xsd|http://www.w3.org/2001/XMLSchema#"}, additions =
  @RdfAddition(predicates ={"http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}, objects = {"td:Thing"})
)
public class MusicPlayer {

  @ObservableProperty
  public String artist = "";

  @ObservableProperty
  public String genre = "";

  @ObservableProperty
  public String play = "";

  @ObservableProperty
  public String title = "";

  @ObservableProperty
  public String volume = "";

  @ObservableProperty
  public String power = "OFF";

  @Action(requestMethod = "PUT", path = "/artist", inputs={"artist|xsd:string"})
  public String setArtist(String artist) {
    this.artist = artist;
    return this.artist;
  }

  @Action(requestMethod = "PUT", path = "/genre", inputs={"genre|xsd:string"})
  public String setGenre(String genre) {
    this.genre = genre;
    return this.genre;
  }

  @Action(requestMethod = "PUT", path = "/play", inputs={"play|xsd:string"})
  public String setPlay(String play) {
    this.play = play;
    return this.play;
  }

  @Action(requestMethod = "PUT", path = "/title", inputs={"title|xsd:string"})
  public String setTitle(String title) {
    this.title = title;
    return this.title;
  }

  @Action(requestMethod = "PUT", path = "/volume", inputs={"volume|xsd:string"})
  public String setVolume(String volume) {
    this.volume = volume;
    return this.volume;
  }

  @Action(requestMethod = "PUT", path = "/power", inputs={"power|xsd:string"})
  public String setPower(String power) {
    this.power = power;
    return this.power;
  }
}
