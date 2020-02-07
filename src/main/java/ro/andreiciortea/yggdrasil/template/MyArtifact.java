package ro.andreiciortea.yggdrasil.template;

import ro.andreiciortea.yggdrasil.template.annotation.*;

// TODO: enable multiple types
@Artifact(type = "Thing", additions =
  @RdfAddition(predicates ={"eve:test1"}, objects = {"eve:obj1"})
)
/*
 * An example Artifact
 */
public class MyArtifact {

  @ObservableProperty(path = "/myProp")
  public int myProperty = 5;

  @ObservableProperty
  public String property = "yay";
  // TODO: add input and output specification
  @Action(requestMethod = "PUT", path = "/myArtifactActions/myMethod", name = "myFancyName", inputs={"paramA|www.test.type", "paramB|www.test.type2"})
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
