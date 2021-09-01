package org.hyperagents.yggdrasil.signifiers.maze;

public class Maze3 extends GeneralMaze {

  @Override
  protected MazeInitializer getInitializer(String mazeUri){
    return new MazeInitializer3(mazeUri);
  }
}
