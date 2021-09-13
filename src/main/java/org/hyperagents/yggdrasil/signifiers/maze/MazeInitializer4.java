package org.hyperagents.yggdrasil.signifiers.maze;

import org.hyperagents.yggdrasil.signifiers.Visibility;
import org.hyperagents.yggdrasil.signifiers.VisibilityImpl;

public class MazeInitializer4 extends MazeInitializer {

  public MazeInitializer4(String mazeUri){
    super();
    addBasicSignifiers(mazeUri);
    Visibility v = new VisibilityMaze4();
    SignifierTuple t1 = new SignifierTuple("from1To3", Util.createPathSignifierUri(mazeUri, 1, 3), v);
    this.signifiers.add(t1);
    SignifierTuple t2 = new SignifierTuple("from3To9", Util.createPathSignifierUri(mazeUri, 3, 9), v);
    this.signifiers.add(t2);
    /*for (int i = 1; i<=9; i++){
      for (int j = 1; j<= 9; j++){
        if (i!=j){
          String name = "exit" + i + j;
          SignifierTuple t = new SignifierTuple(name, Util.createPathSignifierUri(mazeUri,i,j),v);
          this.signifiers.add(t);
        }
      }
    }*/
  }
}
