package ro.andreiciortea.yggdrasil.template;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.Event;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;

// TODO: enable multiple types
@Artifact(type = "Thing")
public class MyArtifact {

  @ObservableProperty(path = "/myProp")
  public int myProperty = 5;

  @ObservableProperty
  public String property = "yay";

  @Action(path = "/myArtifactActions/myMethod", name = "myFancyName")
  public String myMethod(int paramA, String paramB) {
    myProperty += paramA;
    System.out.println("myMethod: " + paramB);
    return someOtherMethod(myProperty);
  }

  public String someOtherMethod(int param) {
    // No action!
    return "Result" + param;
  }

  @Event
  public String theEvent() {
    return "event happened!";
  }
}
