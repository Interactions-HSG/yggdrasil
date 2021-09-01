package org.hyperagents.yggdrasil.signifiers.maze;

public class Maze4 extends GeneralMaze {

  @Override
  protected MazeInitializer getInitializer(String mazeUri){
    return new MazeInitializer4(mazeUri);
  }
}
