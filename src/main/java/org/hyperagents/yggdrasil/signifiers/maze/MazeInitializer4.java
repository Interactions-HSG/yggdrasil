package org.hyperagents.yggdrasil.signifiers.maze;

import org.hyperagents.yggdrasil.signifiers.Visibility;

public class MazeInitializer4 extends MazeInitializer {

  public MazeInitializer4(){
    super();
    Visibility v = new VisibilityMaze4();
    for (int i = 1; i<=9; i++){
      for (int j = 1; j<= 9; j++){
        if (i!=j){
          String name = "exit" + i + j;
          SignifierTuple t = new SignifierTuple(name, Util.createPathSignifier(i,j),v);
          this.signifiers.add(t);
        }
      }
    }
  }
}
