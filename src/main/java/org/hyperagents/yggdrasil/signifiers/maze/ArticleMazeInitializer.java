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

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class ArticleMazeInitializer extends MazeInitializer{
  public ArticleMazeInitializer(String mazeUri) {
    Visibility v = new ArticleMazeVisibility();
    for (int room = 1; room<=9; room++){
      for (int m = 0; m<= 3; m++){
        if (isValid(room,m)){
          String name = "exit" + room + m;
          SignifierTuple t = new SignifierTuple(name, createSignifier(mazeUri,room,m),v);
          this.signifiers.add(t);
        }
      }
    }
  }

  public static Signifier createSignifier(String mazeUri, int room, int m){
    Resource signifierId = RDFS.rdf.createBNode();
    Affordance affordance = createAffordance(mazeUri,room, m);
    Signifier signifier = new Signifier.Builder(signifierId)
      .addAffordance(affordance)
      .build();
    return signifier;
  }

  public static Affordance createAffordance(String mazeUri, int room, int m){
    List<State> states = Util.getStates();
    Resource affordanceId = RDFS.rdf.createBNode();
    Action action = createAction(mazeUri, room, m);
    int toRoom = Util.nextRoom(room, m);
    State postcondition = Util.createObjectiveFromRoomNb(toRoom);
    Affordance affordance = new Affordance.Builder(affordanceId)
      .setPrecondition(states.get(room-1))
      .setPostcondition(postcondition)
      .addObjective(postcondition)
      .addObjective(states.get(8))
      .addAction(action)
      .build();
    return affordance;
  }

  public static Action createAction(String mazeUri, int room, int m){
    Resource actionId = RDFS.rdf.createBNode();
    HypermediaAction action = new HypermediaAction.Builder(actionId, mazeUri+"/move", "POST")
      .setPayload("["+m+"]")
      .build();
    return action;
  }




  public static Map<Integer, List<Integer>> getExitMovements(){
    Map<Integer, List<Integer>> map = new Hashtable<>();
    Integer[] array = {0,1};
    map.put(1, Arrays.asList(array));
    array = new Integer[]{0,1};
    map.put(2, Arrays.asList(array));
    array = new Integer[]{0};
    map.put(3, Arrays.asList(array));
    array = new Integer[]{0};
    map.put(4, Arrays.asList(array));
    array = new Integer[]{0,1};
    map.put(5, Arrays.asList(array));
    array = new Integer[]{0,1};
    map.put(6, Arrays.asList(array));
    array = new Integer[]{1};
    map.put(7, Arrays.asList(array));
    array = new Integer[]{1};
    map.put(8, Arrays.asList(array));
    array = new Integer[]{};
    map.put(9, Arrays.asList(array));
    return map;
  }
}
