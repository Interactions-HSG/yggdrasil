package org.hyperagents.yggdrasil.signifiers.maze;


public class ArticleMaze extends GeneralMaze {

  @Override
  protected MazeInitializer getInitializer(String mazeUri){

    return new ArticleMazeInitializer(mazeUri);
  }
}
