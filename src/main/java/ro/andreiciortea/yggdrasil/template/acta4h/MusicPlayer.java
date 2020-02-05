package ro.andreiciortea.yggdrasil.template.acta4h;

import io.vertx.core.http.HttpMethod;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;

@Artifact(type = "MusicPlayer", additions =
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

  @Action(requestMethod = "PUT", path = "/artist")
  public String setArtist(String artist) {
    this.artist = artist;
    return this.artist;
  }

  @Action(requestMethod = "PUT", path = "/genre")
  public String setGenre(String genre) {
    this.genre = genre;
    return this.genre;
  }

  @Action(requestMethod = "PUT", path = "/play")
  public String setPlay(String play) {
    this.play = play;
    return this.play;
  }

  @Action(requestMethod = "PUT", path = "/title")
  public String setTitle(String title) {
    this.title = title;
    return this.title;
  }

  @Action(requestMethod = "PUT", path = "/volume")
  public String setVolume(String volume) {
    this.volume = volume;
    return this.volume;
  }

  @Action(requestMethod = "PUT", path = "/power")
  public String setPower(String power) {
    this.power = power;
    return this.power;
  }
}
