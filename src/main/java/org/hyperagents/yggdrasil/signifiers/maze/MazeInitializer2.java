package org.hyperagents.yggdrasil.signifiers.maze;

import org.hyperagents.yggdrasil.signifiers.Visibility;

public class MazeInitializer2 extends MazeInitializer {

  public MazeInitializer2(String mazeUri){
    super();
    Visibility v = new VisibilityMaze2();
    SignifierTuple t1 = new SignifierTuple("exit1", Util.createPathSignifierUri(mazeUri,1,9),v);
    this.signifiers.add(t1);
    SignifierTuple t2 = new SignifierTuple("exit2", Util.createPathSignifierUri(mazeUri,2,9),v);
    this.signifiers.add(t2);
    SignifierTuple t3 = new SignifierTuple("exit3", Util.createPathSignifierUri(mazeUri,3,9),v);
    this.signifiers.add(t3);
    SignifierTuple t4 = new SignifierTuple("exit4", Util.createPathSignifierUri(mazeUri,4,9),v);
    this.signifiers.add(t4);
    SignifierTuple t5 = new SignifierTuple("exit5", Util.createPathSignifierUri(mazeUri,5,9),v);
    this.signifiers.add(t5);
    SignifierTuple t6 = new SignifierTuple("exit6", Util.createPathSignifierUri(mazeUri,6,9),v);
    this.signifiers.add(t6);
    SignifierTuple t7 = new SignifierTuple("exit7", Util.createPathSignifierUri(mazeUri,7,9),v);
    this.signifiers.add(t7);
    SignifierTuple t8 = new SignifierTuple("exit8", Util.createPathSignifierUri(mazeUri,8,9),v);
    this.signifiers.add(t8);
  }

}
