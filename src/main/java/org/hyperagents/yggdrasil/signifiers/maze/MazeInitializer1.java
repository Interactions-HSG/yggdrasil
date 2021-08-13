package org.hyperagents.yggdrasil.signifiers.maze;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.hyperagents.signifier.Signifier;
import org.hyperagents.yggdrasil.signifiers.Visibility;
import org.hyperagents.yggdrasil.signifiers.VisibilityImpl;

public class MazeInitializer1 extends MazeInitializer {

  public MazeInitializer1(){
    super();
    //System.out.println("initialize");
    Visibility v = new VisibilityImpl();
    //System.out.println("visibility defined");
    Signifier s= Util.createPathSignifier(1,9);
    //System.out.println("signifier defined");
    SignifierTuple t = new SignifierTuple("exit", s, v);
    //System.out.println("tuple defined");
    this.signifiers.add(t);
    String content = t.getSignifier().getTextTriples(RDFFormat.TURTLE);
    //System.out.println(content);
  }
}
