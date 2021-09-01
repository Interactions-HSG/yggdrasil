package org.hyperagents.yggdrasil.signifiers.maze;

public class Maze1 extends GeneralMaze {

  @Override
  protected MazeInitializer getInitializer(String mazeUri){
    return new MazeInitializer1(mazeUri);
  }
}
