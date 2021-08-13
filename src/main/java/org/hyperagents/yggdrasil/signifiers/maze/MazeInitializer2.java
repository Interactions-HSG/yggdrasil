package org.hyperagents.yggdrasil.signifiers.maze;

import org.hyperagents.yggdrasil.signifiers.Visibility;

public class MazeInitializer2 extends MazeInitializer {

  public MazeInitializer2(){
    super();
    Visibility v = new VisibilityMaze2();
    SignifierTuple t1 = new SignifierTuple("exit1", Util.createPathSignifier(1,9),v);
    this.signifiers.add(t1);
    SignifierTuple t2 = new SignifierTuple("exit2", Util.createPathSignifier(2,9),v);
    this.signifiers.add(t2);
    SignifierTuple t3 = new SignifierTuple("exit3", Util.createPathSignifier(3,9),v);
    this.signifiers.add(t3);
    SignifierTuple t4 = new SignifierTuple("exit4", Util.createPathSignifier(4,9),v);
    this.signifiers.add(t4);
    SignifierTuple t5 = new SignifierTuple("exit5", Util.createPathSignifier(5,9),v);
    this.signifiers.add(t5);
    SignifierTuple t6 = new SignifierTuple("exit6", Util.createPathSignifier(6,9),v);
    this.signifiers.add(t6);
    SignifierTuple t7 = new SignifierTuple("exit7", Util.createPathSignifier(7,9),v);
    this.signifiers.add(t7);
    SignifierTuple t8 = new SignifierTuple("exit8", Util.createPathSignifier(8,9),v);
    this.signifiers.add(t8);
  }
}
