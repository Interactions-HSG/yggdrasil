package org.hyperagents.yggdrasil.signifiers.maze;

import org.eclipse.rdf4j.model.Resource;
import org.hyperagents.affordance.Affordance;
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

  public static Signifier createBasicSignifier(String mazeUri, int room, int m) {
    Resource signifierId = RDFS.rdf.createBNode();
    Affordance affordance = createBasicAffordance(mazeUri, room, m);
    Signifier signifier = new Signifier.Builder(signifierId)
      .addAffordance(affordance)
      .build();
    return signifier;
  }

  public static Affordance createBasicAffordance(String mazeUri, int room, int m) {
    List<State> states = Util.getStates();
    Resource affordanceId = RDFS.rdf.createBNode();
    DirectPlan plan = createBasicPlan(mazeUri, room, m);
    int toRoom = Util.nextRoom(room, m);
    State postcondition = Util.createObjectiveFromRoomNb(toRoom);
    Affordance affordance = new Affordance.Builder(affordanceId)
      .setPrecondition(states.get(room - 1))
      .setPostcondition(postcondition)
      .addObjective(postcondition)
      .addObjective(states.get(8))
      .addPlan(plan)
      .build();
    return affordance;
  }

  public static DirectPlan createBasicPlan(String mazeUri, int room, int m) {
    Resource planId = RDFS.rdf.createBNode();
    DirectPlan plan = new HypermediaPlan.Builder(planId, mazeUri + "/move", "POST")
      .setPayload("[" + m + "]")
      .build();
    return plan;
  }

  public static Set<Signifier> createBasicSignifiers(String mazeUri) {
    Set<Signifier> signifiers = new HashSet<>();
    for (int room = 1; room <= 9; room++) {
      for (int m = 0; m < 4; m++) {
        if (isValid(room, m)) {
          Signifier signifier = createBasicSignifier(mazeUri, room, m);
          signifiers.add(signifier);
        }

      }
    }
    return signifiers;
  }

  public void addBasicSignifiers(String mazeUri) {
    Visibility v = new VisibilityImpl();
    for (int room = 1; room <= 9; room++) {
      for (int m = 0; m < 4; m++) {
        if (isValid(room, m)) {
          Signifier signifier = createBasicSignifier(mazeUri, room, m);
          int newRoom = Util.nextRoom(room, m);
          String name = "basic signifier to go from room " + room + " to room " + newRoom;
          SignifierTuple t = new SignifierTuple(name, signifier, v);
          this.signifiers.add(t);
        }

      }

    }
  }
}


