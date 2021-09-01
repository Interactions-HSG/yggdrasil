package org.hyperagents.yggdrasil.signifiers.maze;

import org.hyperagents.signifier.Signifier;
import org.hyperagents.yggdrasil.signifiers.Visibility;
import org.hyperagents.yggdrasil.signifiers.VisibilityImpl;

public class MazeInitializer1 extends MazeInitializer {

  public MazeInitializer1(String mazeUri){
    super();
    Visibility v = new VisibilityImpl();
    Signifier s= Util.createPathSignifierUri(mazeUri,1,9);
    SignifierTuple t = new SignifierTuple("exit", s, v);
    this.signifiers.add(t);
  }
}
