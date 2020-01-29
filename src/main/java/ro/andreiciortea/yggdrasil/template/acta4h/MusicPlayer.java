package ro.andreiciortea.yggdrasil.template.acta4h;

import io.vertx.core.http.HttpMethod;

import ro.andreiciortea.yggdrasil.template.annotation.RequestMapping;
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

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Artist")
  public String setArtist(String artist) {
    this.artist = artist;
    return this.artist;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Genre")
  public String setGenre(String genre) {
    this.genre = genre;
    return this.genre;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Play")
  public String setPlay(String play) {
    this.play = play;
    return this.play;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Title")
  public String setTitle(String title) {
    this.title = title;
    return this.title;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Volume")
  public String setVolume(String volume) {
    this.volume = volume;
    return this.volume;
  }

  @RequestMapping(httpMethod = HttpMethod.PUT, path = "/Power")
  public String setPower(String power) {
    this.power = power;
    return this.power;
  }
}
