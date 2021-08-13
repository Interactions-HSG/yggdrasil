package org.hyperagents.yggdrasil.signifiers.maze;

import java.util.*;

public class MazeInitializer {

  protected Map<Integer, List<Integer>> movements;

  protected Set<SignifierTuple> signifiers;

  public MazeInitializer(){
    this.movements = getStandardMovements();
    this.signifiers = new HashSet<>();

  }

  public  Map<Integer, List<Integer>> getMovements(){
    return this.movements;
  }



  public  Set<SignifierTuple> getSignifiers(){
    return this.signifiers;
  }



  public static Map<Integer, List<Integer>> getOriginalMovements(){
    Map<Integer, List<Integer>> movements = new HashMap<>();
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
    return movements;

  }

  public static Map<Integer, List<Integer>> getStandardMovements(){
    Map<Integer, List<Integer>> movements = new HashMap<>();
    Integer[] array = {6, 2, null, null};
    movements.put(1, Arrays.asList(array));
    array = new Integer[]{5, 3, null, 1};
    movements.put(2, Arrays.asList(array));
    array = new Integer[]{4, null, null, 2};
    movements.put(3, Arrays.asList(array));
    array = new Integer[]{9, null, 1, 5};
    movements.put(4, Arrays.asList(array));
    array = new Integer[]{8, 4, 2, 6};
    movements.put(5, Arrays.asList(array));
    array = new Integer[]{7, 5, 3, null};
    movements.put(6, Arrays.asList(array));
    array = new Integer[]{null, 8, 6, null};
    movements.put(7, Arrays.asList(array));
    array = new Integer[]{null, 9, 5, 7};
    movements.put(8, Arrays.asList(array));
    array = new Integer[]{null, null, 4, 8};
    movements.put(9, Arrays.asList(array));
    return movements;

  }

}
