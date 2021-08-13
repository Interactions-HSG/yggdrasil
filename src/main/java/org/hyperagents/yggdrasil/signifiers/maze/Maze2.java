package org.hyperagents.yggdrasil.signifiers.maze;

public class Maze2 extends GeneralMaze{

  @Override
  protected MazeInitializer getInitializer(){
    return new MazeInitializer2();
  }
}
