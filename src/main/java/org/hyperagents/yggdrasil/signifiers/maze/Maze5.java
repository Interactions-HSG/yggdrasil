package org.hyperagents.yggdrasil.signifiers.maze;

public class Maze5 extends GeneralMaze {

  @Override
  protected MazeInitializer getInitializer(String mazeUri){
    return new MazeInitializer5(mazeUri);
  }
}
