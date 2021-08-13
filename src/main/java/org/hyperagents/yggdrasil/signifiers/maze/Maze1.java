package org.hyperagents.yggdrasil.signifiers.maze;

public class Maze1 extends GeneralMaze {

  @Override
  protected MazeInitializer getInitializer(){
    return new MazeInitializer1();
  }
}
