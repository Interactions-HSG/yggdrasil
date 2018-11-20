package ro.andreiciortea.yggdrasil.environment;

import org.mapdb.Atomic;

/**
 * class representing an artifact, can be created from w3c thing description
 */

public class Artifact {
  private String id;
  private String name;
  private Object[] security;
  private Property[] properties;
  private Action[] actions;
  private Event[] events;


  public Artifact(String id, String name, Property[] properties, Action[] actions, Event[] events) {
    this.id = id;
    this.name = name;
    this.properties = properties;
    this.actions = actions;
    this.events = events;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Object[] getSecurity() {
    return security;
  }

  public void setSecurity(Object[] security) {
    this.security = security;
  }

  public Property[] getProperties() {
    return properties;
  }

  public void setProperties(Property[] properties) {
    this.properties = properties;
  }

  public Action[] getActions() {
    return actions;
  }

  public void setActions(Action[] actions) {
    this.actions = actions;
  }

  public Event[] getEvents() {
    return events;
  }

  public void setEvents(Event[] events) {
    this.events = events;
  }
}
