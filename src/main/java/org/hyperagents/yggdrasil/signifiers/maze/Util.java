package org.hyperagents.yggdrasil.signifiers.maze;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.hyperagents.affordance.Affordance;
import org.hyperagents.hypermedia.HypermediaPlan;
import org.hyperagents.ontologies.SignifierOntology;
import org.hyperagents.signifier.Signifier;
import org.hyperagents.plan.Plan;
import org.hyperagents.plan.DirectPlan;
import org.hyperagents.plan.SequencePlan;
import org.hyperagents.plan.AffordancePlan;
import org.hyperagents.util.ReifiedStatement;
import org.hyperagents.util.State;

import java.io.CharArrayWriter;
import java.util.*;

public class Util {

  private static ValueFactory rdf = SimpleValueFactory.getInstance();

  public static List<State> getStates() {
    List<State> states = new ArrayList<>();
    Resource thisAgent = rdf.createIRI(SignifierOntology.thisAgent);
    IRI isIn = rdf.createIRI(EnvironmentOntology.isIn);
    Resource room1 = rdf.createIRI(EnvironmentOntology.room1);
    Resource room2 = rdf.createIRI(EnvironmentOntology.room2);
    Resource room3 = rdf.createIRI(EnvironmentOntology.room3);
    Resource room4 = rdf.createIRI(EnvironmentOntology.room4);
    Resource room5 = rdf.createIRI(EnvironmentOntology.room5);
    Resource room6 = rdf.createIRI(EnvironmentOntology.room6);
    Resource room7 = rdf.createIRI(EnvironmentOntology.room7);
    Resource room8 = rdf.createIRI(EnvironmentOntology.room8);
    Resource room9 = rdf.createIRI(EnvironmentOntology.room9);
    Resource room1Id = rdf.createBNode("room1Id");
    Resource state1StatementId = rdf.createBNode("room1S");
    ReifiedStatement state1Statement = new ReifiedStatement(state1StatementId, thisAgent, isIn, room1);
    State state1 = new State.Builder(room1Id).addStatement(state1Statement).build();
    states.add(state1);
    Resource room2Id = rdf.createBNode("room2Id");
    Resource state2StatementId = rdf.createBNode("room2S");
    ReifiedStatement state2Statement = new ReifiedStatement(state2StatementId, thisAgent, isIn, room2);
    State state2 = new State.Builder(room2Id).addStatement(state2Statement).build();
    states.add(state2);
    Resource room3Id = rdf.createBNode("room3Id");
    Resource state3StatementId = rdf.createBNode("room3S");
    ReifiedStatement state3Statement = new ReifiedStatement(state3StatementId, thisAgent, isIn, room3);
    State state3 = new State.Builder(room3Id).addStatement(state3Statement).build();
    states.add(state3);
    Resource room4Id = rdf.createBNode("room4Id");
    Resource state4StatementId = rdf.createBNode("room4S");
    ReifiedStatement state4Statement = new ReifiedStatement(state4StatementId, thisAgent, isIn, room4);
    State state4 = new State.Builder(room4Id).addStatement(state4Statement).build();
    states.add(state4);
    Resource room5Id = rdf.createBNode("room5Id");
    Resource state5StatementId = rdf.createBNode("room5S");
    ReifiedStatement state5Statement = new ReifiedStatement(state5StatementId, thisAgent, isIn, room5);
    State state5 = new State.Builder(room5Id).addStatement(state5Statement).build();
    states.add(state5);
    Resource room6Id = rdf.createBNode("room6Id");
    Resource state6StatementId = rdf.createBNode("room6S");
    ReifiedStatement state6Statement = new ReifiedStatement(state6StatementId, thisAgent, isIn, room6);
    State state6 = new State.Builder(room6Id).addStatement(state6Statement).build();
    states.add(state6);
    Resource room7Id = rdf.createBNode("room7Id");
    Resource state7StatementId = rdf.createBNode("room7S");
    ReifiedStatement state7Statement = new ReifiedStatement(state7StatementId, thisAgent, isIn, room7);
    State state7 = new State.Builder(room7Id).addStatement(state7Statement).build();
    states.add(state7);
    Resource room8Id = rdf.createBNode("room8Id");
    Resource state8StatementId = rdf.createBNode("room8S");
    ReifiedStatement state8Statement = new ReifiedStatement(state8StatementId, thisAgent, isIn, room8);
    State state8 = new State.Builder(room8Id).addStatement(state8Statement).build();
    states.add(state8);
    Resource room9Id = rdf.createBNode("room9Id");
    Resource state9StatementId = rdf.createBNode("room9S");
    ReifiedStatement state9Statement = new ReifiedStatement(state9StatementId, thisAgent, isIn, room9);
    State state9 = new State.Builder(room9Id).addStatement(state9Statement).build();
    states.add(state9);
    return states;

  }

  public static Map<Integer, List<Integer>> getStandardMovements() {
    Map<Integer, List<Integer>> movements = new HashMap<>();
    Integer[] array = {6, 2, null, null};
    movements.put(1, Arrays.asList(array));
    array = new Integer[]{5, 3, null, 1};
    movements.put(2, Arrays.asList(array));
    array = new Integer[]{4, null, null, 2};
    movements.put(3, Arrays.asList(array));
    array = new Integer[]{9, null, 3, 5};
    movements.put(4, Arrays.asList(array));
    array = new Integer[]{8, 4, 2, 6};
    movements.put(5, Arrays.asList(array));
    array = new Integer[]{7, 5, 1, null};
    movements.put(6, Arrays.asList(array));
    array = new Integer[]{null, 8, 6, null};
    movements.put(7, Arrays.asList(array));
    array = new Integer[]{null, 9, 5, 7};
    movements.put(8, Arrays.asList(array));
    array = new Integer[]{null, null, 4, 8};
    movements.put(9, Arrays.asList(array));
    return movements;

  }

  public static int nextRoom(int room, int m) {
    Map<Integer, List<Integer>> movements = getStandardMovements();
    Integer roomInteger = Integer.valueOf(room);
    if (m >= 0 & m <= 3 & movements.containsKey(roomInteger)) {
      return movements.get(roomInteger).get(m).intValue();
      } else {
        System.out.println("The movement is impossible");
    }

    return 0;
  }

  /**
   * @param fromRoomNb
   * @param toRoomNb
   * @return -1 if the two rooms are not adjacent and the direction to go from the first room to the other room if they are adjacent.
   */
  public static int getDirection(int fromRoomNb, int toRoomNb) {
    int d = -1;
    Map<Integer, List<Integer>> movements = getStandardMovements();
    List<Integer> list = movements.get(fromRoomNb);
    if (list.contains(toRoomNb)) {
      d = list.indexOf(toRoomNb);
    }
    return d;
  }

  public static boolean isMovementPossible(int fromRoomNb, int toRoomNb) {
    boolean b = false;
    Map<Integer, List<Integer>> movements = getStandardMovements();
    List<Integer> list = movements.get(fromRoomNb);
    if (list.contains(toRoomNb)) {
      b = true;
    }
    return b;

  }


  public static int getGeneralDirection(int fromRoomNb, int toRoomNb) {
    boolean b = isMovementPossible(fromRoomNb, toRoomNb);
    int d = getDirection(fromRoomNb, toRoomNb);
    if (!b) {
      if (fromRoomNb < toRoomNb) {
        int newToRoomNb = plus(fromRoomNb);
        d = getDirection(fromRoomNb, newToRoomNb);
      } else if (fromRoomNb > toRoomNb) {
        int newToRoomNb = minus(fromRoomNb);
        d = getDirection(fromRoomNb, newToRoomNb);
      }

    }
    return d;

  }

  public static int plus(int room) {
    int newRoom = 9;
    if (room != 9) {
      newRoom = room + 1;
    }
    return newRoom;


  }

  public static int minus(int room) {
    int newRoom = 1;
    if (room != 1) {
      newRoom = room - 1;
    }
    return newRoom;


  }

  public static HypermediaPlan getLocalMovePlan(Resource planId, int fromRoomId, int d) {
    String payload = "[" + d + ",0]";
    HypermediaPlan plan = new HypermediaPlan.Builder(planId, "http://example.org/move", "POST")
      .setPayload(payload)
      .build();
    return plan;
  }

  public static HypermediaPlan getLocalMovePlanUri(String mazeUri, Resource planId, int fromRoomId, int d) {
    String payload = "[" + d + "]";
    String moveUri = mazeUri + "/move";
    System.out.println("move uri: " + moveUri);
    HypermediaPlan plan = new HypermediaPlan.Builder(planId, moveUri, "POST")
      .setPayload(payload)
      .build();
    return plan;
  }

  public static List getLocalMovement(int fromRoomNb, int toRoomNb) {
    List list = new ArrayList();
    int d = getGeneralDirection(fromRoomNb, toRoomNb);
    list.add(nextRoom(fromRoomNb, d));
    list.add(d);
    return list;

  }

  public static Affordance getLocalMoveAffordance(Resource affordanceId, int fromRoomId, int m) {
    List<State> states = getStates();
    State fromRoom = states.get(fromRoomId - 1);
    int toRoomId = nextRoom(fromRoomId, m);
    State toRoom = states.get(toRoomId - 1);
    Resource planId = rdf.createBNode();
    HypermediaPlan plan = getLocalMovePlan(planId, fromRoomId, m);
    Affordance affordance = new Affordance.Builder(affordanceId)
      .setPrecondition(fromRoom)
      .setPostcondition(toRoom)
      .addObjective(toRoom)
      .addPlan(plan)
      .build();
    return affordance;
  }

  public static Affordance getLocalMoveAffordanceUri(String mazeUri, Resource affordanceId, int fromRoomId, int m) {
    List<State> states = getStates();
    State fromRoom = states.get(fromRoomId - 1);
    int toRoomId = nextRoom(fromRoomId, m);
    State toRoom = states.get(toRoomId - 1);
    Resource planId = rdf.createBNode();
    HypermediaPlan plan = getLocalMovePlanUri(mazeUri, planId, fromRoomId, m);
    Affordance affordance = new Affordance.Builder(affordanceId)
      .setPrecondition(fromRoom)
      .setPostcondition(toRoom)
      .addObjective(toRoom)
      .addPlan(plan)
      .build();
    return affordance;
  }

  public static AffordancePlan createAffordancePlanFromRoomNb(Resource planId, int room) {
    State objective = createObjectiveFromRoomNb(room);
    return new AffordancePlan(planId, objective);
  }


  public static DirectPlan getMovePlan(Resource planId, int fromRoomId, int toRoomId) {
    List<State> states = getStates();
    DirectPlan plan = null;
    if (fromRoomId == toRoomId) {
      ;
    } else {
      int d = getDirection(fromRoomId, toRoomId);
      if (d != -1) {
        plan = getLocalMovePlan(planId, fromRoomId, d);

      } else {
        SequencePlan.Builder builder = new SequencePlan.Builder(planId);
        List<Plan> sequence = new ArrayList<>();
        int i = 0;
        while (fromRoomId != toRoomId) {
          i++;
          List<Integer> list = getLocalMovement(fromRoomId, toRoomId);
          int newRoom = list.get(0).intValue();
          int m = list.get(1).intValue();
          Resource affordanceId = rdf.createBNode("from" + fromRoomId + "to" + newRoom + "affordance");
          Resource affordancePlanId = rdf.createBNode("from" + fromRoomId + "to" + newRoom + "affordancePlan");
          Affordance affordance = getLocalMoveAffordance(affordanceId, fromRoomId, m);
          Plan affordancePlan = createAffordancePlanFromRoomNb(affordancePlanId, newRoom);
          fromRoomId = newRoom;
          sequence.add(affordancePlan);

        }
        builder.addSequence(sequence);
        plan = builder.build();


      }
    }
    return plan;
  }

  public static DirectPlan getMovePlanUri(String mazeUri, Resource planId, int fromRoomId, int toRoomId) {
    List<State> states = getStates();
    DirectPlan plan = null;
    if (fromRoomId == toRoomId) {
    } else {
      int d = getDirection(fromRoomId, toRoomId);
      if (d != -1) {
        plan = getLocalMovePlanUri(mazeUri, planId, fromRoomId, d);

      } else {
        SequencePlan.Builder builder = new SequencePlan.Builder(planId);
        List<Plan> sequence = new ArrayList<>();
        int i = 0;
        while (fromRoomId != toRoomId) {
          i++;
          List<Integer> list = getLocalMovement(fromRoomId, toRoomId);
          int newRoom = list.get(0).intValue();
          int m = list.get(1).intValue();
          Resource affordanceId = rdf.createBNode("from" + fromRoomId + "to" + newRoom + "affordance");
          Affordance affordance = getLocalMoveAffordanceUri(mazeUri, affordanceId, fromRoomId, m);
          Resource affordancePlanId = rdf.createBNode("from" + fromRoomId + "to" + newRoom + "affordancePlan");
          Plan affordancePlan = createAffordancePlanFromRoomNb(affordancePlanId, newRoom);
          fromRoomId = newRoom;
          sequence.add(affordancePlan);

        }
        builder.addSequence(sequence);
        plan = builder.build();


      }
    }
    return plan;
  }

  public static Affordance getMoveAffordance(Resource affordanceId, int fromRoomNb, int toRoomNb) {
    List<State> states = getStates();
    State fromRoom = states.get(fromRoomNb - 1);
    State toRoom = states.get(toRoomNb - 1);
    Resource planId = rdf.createBNode();
    DirectPlan movePlan = getMovePlan(planId, fromRoomNb, toRoomNb);
    Affordance affordance = new Affordance.Builder(affordanceId)
      .setPrecondition(fromRoom)
      .setPostcondition(toRoom)
      .addObjective(toRoom)
      .addPlan(movePlan)
      .build();
    return affordance;
  }

  public static Affordance getMoveAffordanceUri(String mazeUri, Resource affordanceId, int fromRoomNb, int toRoomNb) {
    List<State> states = getStates();
    State fromRoom = states.get(fromRoomNb - 1);
    State toRoom = states.get(toRoomNb - 1);
    Resource planId = rdf.createBNode();
    DirectPlan movePlan = getMovePlanUri(mazeUri, planId, fromRoomNb, toRoomNb);
    Affordance affordance = new Affordance.Builder(affordanceId)
      .setPrecondition(fromRoom)
      .setPostcondition(toRoom)
      .addObjective(toRoom)
      .addPlan(movePlan)
      .build();
    return affordance;
  }

  public static Signifier createSignifierFromTo(Resource signifierId, Resource affordanceId, int fromRoomNb, int toRoomNb) {
    Signifier.Builder builder = new Signifier.Builder(signifierId);
    Affordance exit = getMoveAffordance(affordanceId, fromRoomNb, toRoomNb);
    builder.addAffordance(exit);
    return builder.build();
  }

  public static Signifier createSignifierFromToUri(String mazeUri, Resource signifierId, Resource affordanceId, int fromRoomNb, int toRoomNb) {
    Signifier.Builder builder = new Signifier.Builder(signifierId);
    Affordance exit = getMoveAffordanceUri(mazeUri, affordanceId, fromRoomNb, toRoomNb);
    builder.addAffordance(exit);
    return builder.build();
  }

  public static Signifier createPathSignifier(int fromRoom, int toRoom) {
    String signifierName = "signifier" + fromRoom + toRoom;
    Resource signifierId = rdf.createBNode(signifierName);
    String affordanceName = "exit" + fromRoom + toRoom;
    Resource affordanceId = rdf.createBNode(affordanceName);
    return createSignifierFromTo(signifierId, affordanceId, fromRoom, toRoom);
  }

  public static Signifier createPathSignifierUri(String mazeUri, int fromRoom, int toRoom) {
    String signifierName = "signifier" + fromRoom + toRoom;
    Resource signifierId = rdf.createBNode(signifierName);
    String affordanceName = "exit" + fromRoom + toRoom;
    Resource affordanceId = rdf.createBNode(affordanceName);
    return createSignifierFromToUri(mazeUri, signifierId, affordanceId, fromRoom, toRoom);
  }

  public static void displayModel(Model m) {
    CharArrayWriter writer = new CharArrayWriter();
    Rio.write(m, writer, RDFFormat.TURTLE);
    System.out.println(writer.toCharArray());
  }

  public static IRI getIRIFromRoomNb(int room) {
    IRI roomIRI = rdf.createIRI(EnvironmentOntology.room9);
    if (room == 1) {
      roomIRI = rdf.createIRI(EnvironmentOntology.room1);
    } else if (room == 2) {
      roomIRI = rdf.createIRI(EnvironmentOntology.room2);
    } else if (room == 3) {
      roomIRI = rdf.createIRI(EnvironmentOntology.room3);
    } else if (room == 4) {
      roomIRI = rdf.createIRI(EnvironmentOntology.room4);
    } else if (room == 5) {
      roomIRI = rdf.createIRI(EnvironmentOntology.room5);
    } else if (room == 6) {
      roomIRI = rdf.createIRI(EnvironmentOntology.room6);
    } else if (room == 7) {
      roomIRI = rdf.createIRI(EnvironmentOntology.room7);
    } else if (room == 8) {
      roomIRI = rdf.createIRI(EnvironmentOntology.room8);
    }
    return roomIRI;
  }

  public static State createObjectiveFromRoomNb(int room) {
    IRI roomIRI = getIRIFromRoomNb(room);
    ReifiedStatement statement = new ReifiedStatement(rdf.createBNode("goToRoom" + room + "statement"), rdf.createIRI(SignifierOntology.thisAgent), rdf.createIRI(EnvironmentOntology.isIn), roomIRI);
    Resource stateId = rdf.createBNode("goToRoom" + room);
    State objective = new State.Builder(stateId)
      .addStatement(statement)
      .build();
    return objective;
  }
}
