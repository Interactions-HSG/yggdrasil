package ro.andreiciortea.yggdrasil.template.acta4h;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;

@Artifact(type = "MusicPlayer")
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

  @Action(path = "/actions/setArtist")
  public String setArtist(String artist) {
    this.artist = artist;
    return this.artist;
  }

  @Action(path = "/actions/setGenre")
  public String setGenre(String genre) {
    this.genre = genre;
    return this.genre;
  }

  @Action(path = "/actions/setPlay")
  public String setPlay(String play) {
    this.play = play;
    return this.play;
  }

  @Action(path = "/actions/setTitle")
  public String setTitle(String title) {
    this.title = title;
    return this.title;
  }

  @Action(path = "/actions/setVolume")
  public String setVolume(String volume) {
    this.volume = volume;
    return this.volume;
  }

  @Action(path = "/actions/setPower")
  public String setPower(String power) {
    this.power = power;
    return this.power;
  }
}
