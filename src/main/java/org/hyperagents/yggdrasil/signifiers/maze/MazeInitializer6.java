package org.hyperagents.yggdrasil.signifiers.maze;

import org.eclipse.rdf4j.model.Resource;
import org.hyperagents.affordance.Affordance;
import org.hyperagents.hypermedia.HypermediaPlan;
import org.hyperagents.signifier.Signifier;
import org.hyperagents.util.Plan;
import org.hyperagents.util.RDFS;
import org.hyperagents.yggdrasil.signifiers.Visibility;
import org.hyperagents.yggdrasil.signifiers.VisibilityImpl;

import java.util.*;

public class MazeInitializer6 extends MazeInitializer {
// This maze initializer concerns the case where the signifiers are all visible and corresponds to exit signs.
  public MazeInitializer6(String mazeUri){
    super();
    Visibility v = new VisibilityImpl();
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

  public static boolean isValid(int room, int m){
    boolean b = false;
    Map<Integer, List<Integer>> map = getExitMovements();
    b = map.get(room).contains(m);
    return b;
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
    Resource affordanceId = RDFS.rdf.createBNode();
    Plan plan = createPlan(mazeUri, room, m);
    Affordance affordance = new Affordance.Builder(affordanceId)
      .addPlan(plan)
      .build();
    return affordance;
  }

  public static Plan createPlan(String mazeUri, int room, int m){
    Resource planId = RDFS.rdf.createBNode();
    Plan plan = new HypermediaPlan.Builder(planId, mazeUri+"/move", "POST")
      .setPayload("["+room+",0]")
      .build();
    return plan;
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
