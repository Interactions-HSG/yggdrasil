package org.hyperagents.yggdrasil.signifiers.maze;

public class Maze6 extends GeneralMaze{

  @Override
  protected MazeInitializer getInitializer(String mazeUri){

    return new MazeInitializer6(mazeUri);
  }
}

