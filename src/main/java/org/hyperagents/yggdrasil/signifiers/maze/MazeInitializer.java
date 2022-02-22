package org.hyperagents.yggdrasil.signifiers.maze;

import org.eclipse.rdf4j.model.Resource;
import org.hyperagents.action.Action;
import org.hyperagents.affordance.Affordance;
import org.hyperagents.hypermedia.HypermediaAction;
import org.hyperagents.hypermedia.HypermediaPlan;
import org.hyperagents.plan.DirectPlan;
import org.hyperagents.signifier.Signifier;
import org.hyperagents.util.RDFS;
import org.hyperagents.util.State;
import org.hyperagents.yggdrasil.signifiers.Visibility;
import org.hyperagents.yggdrasil.signifiers.VisibilityImpl;

import java.util.*;

public class MazeInitializer {


  protected Map<Integer, List<Integer>> movements;

  protected Set<SignifierTuple> signifiers;

  public MazeInitializer() {
    this.movements = getStandardMovements();
    this.signifiers = new HashSet<>();

  }

  public Map<Integer, List<Integer>> getMovements() {
    return this.movements;
  }


  public Set<SignifierTuple> getSignifiers() {
    return this.signifiers;
  }


  public static Map<Integer, List<Integer>> getOriginalMovements() {
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

  public static Map<Integer, List<Integer>> getStandardMovements() {
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

  public static Map<Integer, List<Integer>> getExitMovements() {
    Map<Integer, List<Integer>> map = new Hashtable<>();
    Integer[] array = {0, 1};
    map.put(1, Arrays.asList(array));
    array = new Integer[]{0, 1};
    map.put(2, Arrays.asList(array));
    array = new Integer[]{0};
    map.put(3, Arrays.asList(array));
    array = new Integer[]{0};
    map.put(4, Arrays.asList(array));
    array = new Integer[]{0, 1};
    map.put(5, Arrays.asList(array));
    array = new Integer[]{0, 1};
    map.put(6, Arrays.asList(array));
    array = new Integer[]{1};
    map.put(7, Arrays.asList(array));
    array = new Integer[]{1};
    map.put(8, Arrays.asList(array));
    array = new Integer[]{};
    map.put(9, Arrays.asList(array));
    return map;
  }

  public static boolean isValid(int room, int m) {
    boolean b = false;
    Map<Integer, List<Integer>> map = getExitMovements();
    b = map.get(room).contains(m);
    return b;
  }

  public static boolean isStandard(int room, int m) {
    boolean b = false;
    Map<Integer, List<Integer>> map = getStandardMovements();
    Integer roomInteger = Integer.valueOf(room);
    System.out.println(roomInteger);
    List<Integer> list = map.get(roomInteger);
    if (m>=0 && m<=3){
      Integer integer = list.get(m);
      if (integer !=null){
        b = true;
      }
    }
    return b;
  }

  public static Signifier createBasicSignifier(String mazeUri, int room, int m) {
    int toRoom = Util.nextRoom(room, m);
    Resource signifierId = RDFS.rdf.createBNode("signifier"+room+toRoom);
    Affordance affordance = createBasicAffordance(mazeUri, room, m);
    Signifier signifier = new Signifier.Builder(signifierId)
      .addAffordance(affordance)
      .build();
    return signifier;
  }

  public static Affordance createBasicAffordance(String mazeUri, int room, int m) {
    List<State> states = Util.getStates();
    int toRoom = Util.nextRoom(room, m);
    Resource affordanceId = RDFS.rdf.createBNode("affordance"+room+toRoom);
    HypermediaPlan plan = createBasicPlan(mazeUri, m);
    Action action = createBasicAction(mazeUri, m);
    State postcondition = Util.createObjectiveFromRoomNb(toRoom);
    Affordance affordance = new Affordance.Builder(affordanceId)
      .setPrecondition(states.get(room - 1))
      .setPostcondition(postcondition)
      .addObjective(postcondition)
      .addAction(action)
      .build();
    return affordance;
  }

  public static HypermediaAction createBasicAction(String mazeUri, int m){
    Resource actionId = RDFS.rdf.createBNode("movement " + m);
    HypermediaAction action = new HypermediaAction.Builder(actionId, mazeUri + "/move", "POST")
      .setPayload("[" + m + "]")
      .build();
    return action;
  }

  public static HypermediaPlan createBasicPlan(String mazeUri, int m) {
    Resource planId = RDFS.rdf.createBNode("movement"+m);
    HypermediaPlan plan = new HypermediaPlan.Builder(planId, mazeUri + "/move", "POST")
      .setPayload("[" + m + "]")
      .build();
    return plan;
  }

  public static Set<Signifier> createBasicSignifiers(String mazeUri) {
    Set<Signifier> signifiers = new HashSet<>();
    for (int room = 1; room <= 9; room++) {
      for (int m = 0; m < 4; m++) {
        if (isStandard(room, m)) {
          Signifier signifier = createBasicSignifier(mazeUri, room, m);
          signifiers.add(signifier);
        }

      }
    }
    return signifiers;
  }

  public void addBasicSignifiers(String mazeUri) {
    Visibility v = new SimpleMazeVisibility();
    for (int room = 1; room <= 9; room++) {
      for (int m = 0; m < 4; m++) {
        if (isStandard(room, m)) {
          System.out.println("standard");
          System.out.println("from room: "+room);
          System.out.println("has movement: "+m);
          Signifier signifier = createBasicSignifier(mazeUri, room, m);
          int newRoom = Util.nextRoom(room, m);
          System.out.println("to room: "+newRoom);
          String name = "basic signifier to go from room " + room + " to room " + newRoom;
          SignifierTuple t = new SignifierTuple(name, signifier, v);
          this.signifiers.add(t);
        }

      }

    }
  }
}


