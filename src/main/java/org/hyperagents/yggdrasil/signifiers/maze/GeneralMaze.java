package org.hyperagents.yggdrasil.signifiers.maze;

import cartago.AgentId;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.schemas.IntegerSchema;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.hyperagents.yggdrasil.signifiers.SignifierHypermediaArtifact;

import java.util.*;

public class GeneralMaze extends SignifierHypermediaArtifact {


  private Map<AgentId, Integer> locations;

  private Map<AgentId, Integer> goals;

  //0: North, 1: West, 2: South, 3: East
  private Map<Integer, List<Integer>> movements;

  private ValueFactory rdf;

  public void init(){
    System.out.println("start init");
    rdf = SimpleValueFactory.getInstance();
    locations = new HashMap<>();
    movements = new HashMap<>();
    goals = new HashMap<>();
    System.out.println("first step");
    MazeInitializer initializer = getInitializer();
    System.out.println("initializer");
    this.movements = initializer.getMovements();
    Iterator<SignifierTuple> iterator = initializer.getSignifiers().iterator();
    while (iterator.hasNext()){
      SignifierTuple t = iterator.next();
      this.registry.addSignifier(rdf.createIRI(t.getName()), t.toTuple());
    }
    System.out.println("end init");
  }

  protected MazeInitializer getInitializer(){
    return new MazeInitializer();
  }

  @OPERATION
  public void register(){
    AgentId agent = this.getCurrentOpAgentId();
    this.locations.put(agent, 1);
    this.goals.put(agent, 9);
    System.out.println("registration done");
  }

  /**
   * The agent can select where he wants to enter the maze.
   * @param entrance
   */
  @OPERATION
  public void register(int entrance){
    AgentId agent = this.getCurrentOpAgentId();
    this.locations.put(agent, entrance);
    this.goals.put(agent, 9);
  }

  @OPERATION
  public void register(int entrance, int goal){
    AgentId agent = this.getCurrentOpAgentId();
    this.locations.put(agent, entrance);
    this.goals.put(agent, goal);

  }


  @OPERATION
  public void move(int m){
    AgentId agent = this.getCurrentOpAgentId();
    int room = this.locations.get(agent).intValue();
    int newRoom = nextRoom(room, m);
    this.locations.put(agent, newRoom);
    System.out.println("new  room : "+newRoom);
    if (newRoom == this.goals.get(agent)){
      System.out.println("Victory for agent: "+agent);
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

  @OPERATION
  public void getCurrentRoom(OpFeedbackParam<Object> param){
    AgentId agent = this.getCurrentOpAgentId();
    Integer room = locations.get(agent);
    param.set(room);
  }



  @Override
  public Model getState(){
    MazeState.Builder builder = new MazeState.Builder();
    Set<AgentId> agents = locations.keySet();
    for (AgentId agent : agents){
      builder.addLocation(agent.getAgentName(), locations.get(agent));
    }
    agents = goals.keySet();
    for (AgentId agent : agents){
      builder.addGoal(agent.getAgentName(), locations.get(agent));
    }
    MazeState state = builder.build();
    return state.getModel();

  }



  @Override
  protected void registerInteractionAffordances() {
    registerSignifierAffordances();
    registerActionAffordance("http://example.org/register", "register", "/register");
    DataSchema intParameter = new IntegerSchema.Builder().build();
    registerActionAffordance("http://example.org/move", "move", "/move",intParameter);
    registerActionAffordance("http://example.org/available", "availableRooms", "/available", intParameter);
    registerActionAffordance("http://example.org/current", "getCurrentRoom", "/current");

  }

}
