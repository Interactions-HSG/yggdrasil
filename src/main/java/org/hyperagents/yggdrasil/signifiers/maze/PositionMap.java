package org.hyperagents.yggdrasil.signifiers.maze;

import java.util.HashMap;
import java.util.Map;

public class PositionMap {

  private Map<Position, Boolean> map;

  public PositionMap(){
    this.map = new HashMap<>();
  }

  public void add(int x, int y, boolean b){
    this.map.put(new Position(x,y), Boolean.valueOf(b));
  }

  public boolean get(int x, int y){
    boolean b = false;
    for (Position p: map.keySet()){
      if (p.getX()==x && p.getY()==y){
        b = map.get(p).booleanValue();
      }
    }
    return b;
  }

  public boolean hasKeys(int x, int y){
    boolean b = false;
    for (Position p: map.keySet()){
      if (p.getX()==x && p.getY()==y){
        b = true;
      }
    }

    return b;
  }

  private class Position{
    private int x;

    private int y;

    public Position(int x, int y){
      this.x = x;
      this.y = y;
    }

    public int getX(){
      return this.x;
    }

    public int getY(){
      return this.y;
    }

    @Override
    public String toString(){
      return "x: "+x+"; y: "+y;
    }

    @Override
    public boolean equals(Object obj){
      boolean b = false;
      Position p = (Position) obj;
      if (p.getX()== x && p.getY() == y){
        b = true;
      }
      return b;
    }
  }

}

