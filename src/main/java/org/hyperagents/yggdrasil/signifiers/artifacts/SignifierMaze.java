package org.hyperagents.yggdrasil.signifiers.artifacts;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignifierMaze {

  private Map<String, Integer> locations;

  //0: North, 1: West, 2: South, 3: East
  private Map<Integer, List<Integer>> movements;

  private ValueFactory rdf;

  public void init(){
    rdf = SimpleValueFactory.getInstance();
    locations = new HashMap<>();
    movements = new HashMap<>();
    Integer[] array = {4, 2, null, null};
    movements.put(1, Arrays.asList(array));
    array = new Integer[]{5, 3, null, 1};
    movements.put(2, Arrays.asList(array));
    array = new Integer[]{6, null, null, 2};
    movements.put(3, Arrays.asList(array));
    array = new Integer[]{7, 5, 1, null};
    movements.put(4, Arrays.asList(array));
    array = new Integer[]{8, 6, 2, 4};
    movements.put(5, Arrays.asList(array));
    array = new Integer[]{9, null, 3, 5};
    movements.put(6, Arrays.asList(array));
    array = new Integer[]{null, 8, 4, null};
    movements.put(7, Arrays.asList(array));
    array = new Integer[]{null, 9, 5, 7};
    movements.put(8, Arrays.asList(array));
    array = new Integer[]{null, null, 6, 8};
    movements.put(9, Arrays.asList(array));
  }

@OPERATION
  public void move(String agent,int m){
    int room = this.locations.get(agent).intValue();
    int newRoom = nextRoom(room, m);
    this.locations.put(agent, newRoom);
    System.out.println("new  room : "+newRoom);
    if (newRoom == 9){
      System.out.println("Victory for agent "+agent);
    }
  }

  private int nextRoom(int room,int m){
    int newRoom=room;
    if (m>=0 & m<=3 & this.movements.containsKey(room)) {
      List<Integer> rooms = this.movements.get(room);
      Integer result = rooms.get(m);
      if (result != null) {
        newRoom = result.intValue();
        return newRoom;
      }
    }
    System.out.println("invalid movement");
    return newRoom;
  }

  @OPERATION
  public void availableRooms(int room, OpFeedbackParam<List<Integer>> result){
    List<Integer> list = this.movements.get(room);
    result.set(list);

  }
}

