package org.hyperagents.yggdrasil.signifiers.maze;

import cartago.Tuple;
import org.hyperagents.signifier.Signifier;
import org.hyperagents.yggdrasil.signifiers.Visibility;

public class SignifierTuple {

  private String name;

  private Signifier signifier;

  private Visibility visibility;

  public SignifierTuple(String name, Signifier signifier, Visibility visibility){
    this.name = name;
    this.signifier = signifier;
    this.visibility = visibility;
  }

  public String getName(){
    return this.name;
  }

  public Signifier getSignifier(){
    return this.signifier;
  }

  public Visibility getVisibility(){
    return this.visibility;
  }

  public Tuple toTuple(){
    return new Tuple(name, signifier, visibility);
  }
}
