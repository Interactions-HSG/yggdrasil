package ro.andreiciortea.yggdrasil.template.acta4h;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;

@Artifact(type = "Rollershutter")
public class Rollershutter {

  @ObservableProperty
  public double percentage = 0.0;

  @Action(path = "/actions/setPercentage")
  public double setPercentage(double percentage) {
    this.percentage = percentage;
    return this.percentage;
  }
}
