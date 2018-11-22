package ro.andreiciortea.yggdrasil.template;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.Event;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;

// TODO: enable multiple types
@Artifact(type = "Thing")
public class MyArtifact {

  @ObservableProperty
  public int myProperty;

  @ObservableProperty
  public String property;

  @Action(path = "myArtifactActions/myMethod", name = "myFancyName")
  public String myMethod(int numberInput) {
    myProperty += 1;
    return someOtherMethod();
  }

  public String someOtherMethod() {
    // No action!
    return "yay";
  }

  @Event
  public String theEvent() {
    return "event happened!";
  }
}
