package org.hyperagents.yggdrasil.signifiers.maze;

import org.eclipse.rdf4j.model.Resource;
import org.hyperagents.affordance.Affordance;
import org.hyperagents.hypermedia.HypermediaPlan;
import org.hyperagents.ontologies.SignifierOntology;
import org.hyperagents.signifier.Signifier;
import org.hyperagents.util.Plan;
import org.hyperagents.util.RDFS;
import org.hyperagents.util.ReifiedStatement;
import org.hyperagents.util.State;
import org.hyperagents.yggdrasil.signifiers.Visibility;
import org.hyperagents.yggdrasil.signifiers.VisibilityImpl;

import java.util.*;

public class MazeInitializer7 extends MazeInitializer {
  // This maze initializer concerns the case where the affordances have objectives indicating their uses.
  public MazeInitializer7(String mazeUri){
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
    Affordance.Builder builder = new Affordance.Builder(affordanceId)
      .addPlan(plan);
    Set<State> objectives = getObjectives(room,m);
    for (State objective: objectives){
      //builder.addObjective(objective);
    }
    Affordance affordance = builder.build();
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

  public static Set<State> getObjectives(int room, int m){
    Set<State> objectives = new HashSet<>();
    List<State> states = getAllStates();
    if (room == 1 && m == 0){
    }
    return objectives;
  }

  public static List<State> getAllStates(){
    List<State> states = new ArrayList<>();
    Resource state1Id = RDFS.rdf.createBNode();
    Resource statement1Id = RDFS.rdf.createBNode();
    ReifiedStatement statement1 = new ReifiedStatement(statement1Id, RDFS.rdf.createIRI(SignifierOntology.thisAgent),
      RDFS.rdf.createIRI(EnvironmentOntology.isIn), RDFS.rdf.createIRI(EnvironmentOntology.room1));
    State state1 = new State.Builder(state1Id)
      .addStatement(statement1)
      .build();
    states.add(state1);
    return states;
  }
}



