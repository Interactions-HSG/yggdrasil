package org.hyperagents.yggdrasil.signifiers.maze;

public class Maze3 extends GeneralMaze {

  @Override
  protected MazeInitializer getInitializer(){
    return new MazeInitializer3();
  }
}
