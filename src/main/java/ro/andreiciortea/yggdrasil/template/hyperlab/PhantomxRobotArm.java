package ro.andreiciortea.yggdrasil.template.hyperlab;

import io.vertx.core.http.HttpMethod;

import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.Artifact;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import ro.andreiciortea.yggdrasil.template.annotation.RdfAddition;

@Artifact(types = { "td:Thing", "eve:Artifact" },
          prefixes = {
            "td|http://www.w3.org/ns/td#",
            "xsd|http://www.w3.org/2001/XMLSchema#",
            "iot|http://iotschema.org/",
            "http|http://iotschema.org/protocol/http",
            "eve|http://w3id.org/eve#",
            "ex|http://example.com/"
          },
          additions = @RdfAddition(predicates ={"td:name", "td:base"}, objects = {"Robot3", "http://192.168.2.50/"})
)
public class PhantomxRobotArm {

  @Action(requestMethod = "PUT", path = "https://api.interactions.ics.unisg.ch/leubot/wrist/rotation", inputs={"value|td:Number"})
  public void rotate(double value) {

  }

  @Action(requestMethod = "PUT", path = "https://api.interactions.ics.unisg.ch/leubot/gripper", inputs={"value|td:Number"})
  public void gripper(double value){

  }
}
