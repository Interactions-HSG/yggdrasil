package ro.andreiciortea.yggdrasil.template.acta4h;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;

@Artifact(type = "Rollershutter")
public class Rollershutter {
  public double percentage = 0.0;

  @Action(path = "/actions/setPercentage")
  public double setPercentage(double percentage) {
    this.percentage = percentage;
    return this.percentage;
  }
}
