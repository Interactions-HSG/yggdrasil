package org.hyperagents.yggdrasil.signifiers.maze;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.hyperagents.util.RDFS;

import java.util.Optional;
import java.util.Set;

public class MazeState {

  private Resource locations;
  private Resource goals;

  private Model model;

  protected MazeState(Resource locations, Resource goals,Model model) {
    this.locations = locations;
    this.goals = goals;
    this.model = model;
  }

  public Model getModel(){
    return model;
  }

  public static void addMap(ModelBuilder builder, Resource mapId, String key, Value value){
    Resource elementId = RDFS.rdf.createBNode();
    builder.add(mapId, RDFS.rdf.createIRI(EnvironmentOntology.hasElement), elementId);
    builder.add(elementId, RDFS.rdf.createIRI(EnvironmentOntology.hasKey), key);
    builder.add(elementId, RDFS.rdf.createIRI(EnvironmentOntology.hasValue), value);
  }

  public int  getLocation(String key){
    int location = 0;
    Set<Resource> elements = Models.objectResources(model.filter(locations, RDFS.rdf.createIRI(EnvironmentOntology.hasElement), null));
    for (Resource element : elements){
      Optional<Literal> keyElement = Models.objectLiteral(model.filter(element,RDFS.rdf.createIRI(EnvironmentOntology.hasKey), null ));
      if (keyElement.isPresent()){
        String s = keyElement.get().stringValue();
        if (s.equals(key)){
          Optional<Literal> valueElement = Models.objectLiteral(model.filter(element,RDFS.rdf.createIRI(EnvironmentOntology.hasValue), null ));
          if (valueElement.isPresent()){
            location = valueElement.get().intValue();
          }
        }
      }
    }
    return location;
  }
  public int  getGoal(String key){
    int goal = 0;
    Set<Resource> elements = Models.objectResources(model.filter(goals, RDFS.rdf.createIRI(EnvironmentOntology.hasElement), null));
    for (Resource element : elements){
      Optional<Literal> keyElement = Models.objectLiteral(model.filter(goals,RDFS.rdf.createIRI(EnvironmentOntology.hasKey), null ));
      if (keyElement.isPresent()){
        String s = keyElement.get().stringValue();
        if (s.equals(key)){
          Optional<Literal> valueElement = Models.objectLiteral(model.filter(goals,RDFS.rdf.createIRI(EnvironmentOntology.hasValue), null ));
          if (valueElement.isPresent()){
            goal = valueElement.get().intValue();
          }
        }
      }
    }
    return goal;
  }

  public static MazeState createState(Model model){
    MazeState.Builder builder = new MazeState.Builder();
    Optional<Resource> optionalLocations = Models.subject(model.filter(null, RDF.TYPE, RDFS.rdf.createIRI(EnvironmentOntology.Locations)));
    Optional<Resource> optionalGoals = Models.subject(model.filter(null, RDF.TYPE, RDFS.rdf.createIRI(EnvironmentOntology.Goals)));
    if (optionalLocations.isPresent() && optionalGoals.isPresent()){
      Resource locations = optionalLocations.get();
      Resource goals = optionalGoals.get();
      builder = new MazeState.Builder(locations, goals);
      Set<Resource> locationElements = Models.objectResources(
        model.filter(locations, RDFS.rdf.createIRI(EnvironmentOntology.hasElement),null));
      for (Resource locationElement : locationElements){
        Optional<Literal> optionalKey = Models.objectLiteral(model.filter(locationElement, RDFS.rdf.createIRI(EnvironmentOntology.hasKey),null));
        Optional<Literal> optionalValue = Models.objectLiteral(model.filter(locationElement, RDFS.rdf.createIRI(EnvironmentOntology.hasValue),null));
        if (optionalKey.isPresent() && optionalValue.isPresent()){
          String key = optionalKey.get().stringValue();
          int value = optionalValue.get().intValue();
          builder.addLocation(key, value);
        }
      }
      Set<Resource> goalElements = Models.objectResources(
        model.filter(goals, RDFS.rdf.createIRI(EnvironmentOntology.hasElement),null));
      for (Resource goalElement : goalElements){
        Optional<Literal> optionalKey = Models.objectLiteral(model.filter(goalElement, RDFS.rdf.createIRI(EnvironmentOntology.hasKey),null));
        Optional<Literal> optionalValue = Models.objectLiteral(model.filter(goalElement, RDFS.rdf.createIRI(EnvironmentOntology.hasValue),null));
        if (optionalKey.isPresent() && optionalValue.isPresent()) {
          String key = optionalKey.get().stringValue();
          int value = optionalValue.get().intValue();
          builder.addGoal(key, value);
        }

      }

    }
    return builder.build();


  }

  public static class Builder {
    ModelBuilder modelBuilder;
    Resource locations;
    Resource goals;

    public Builder() {
      this.modelBuilder = new ModelBuilder();
      locations = RDFS.rdf.createBNode("locations");
      goals = RDFS.rdf.createBNode("goals");
      modelBuilder.add(RDFS.rdf.createIRI(EnvironmentOntology.thisArtifact),
        RDFS.rdf.createIRI(EnvironmentOntology.hasLocations), locations);
      modelBuilder.add(locations, RDF.TYPE, RDFS.rdf.createIRI(EnvironmentOntology.Locations));
      modelBuilder.add(RDFS.rdf.createIRI(EnvironmentOntology.thisArtifact),
        RDFS.rdf.createIRI(EnvironmentOntology.hasGoals), goals);
      modelBuilder.add(goals, RDF.TYPE, RDFS.rdf.createIRI(EnvironmentOntology.Goals));
    }

    public Builder(Resource locations, Resource goals){
      this.modelBuilder = new ModelBuilder();
      this.locations = locations;
      this.goals = goals;
      modelBuilder.add(RDFS.rdf.createIRI(EnvironmentOntology.thisArtifact),
        RDFS.rdf.createIRI(EnvironmentOntology.hasLocations), locations);
      modelBuilder.add(locations, RDF.TYPE, RDFS.rdf.createIRI(EnvironmentOntology.Locations));
      modelBuilder.add(RDFS.rdf.createIRI(EnvironmentOntology.thisArtifact),
        RDFS.rdf.createIRI(EnvironmentOntology.hasGoals), goals);
      modelBuilder.add(goals, RDF.TYPE, RDFS.rdf.createIRI(EnvironmentOntology.Goals));

    }

    public Builder addLocation(String name, int n) {
      Value v = RDFS.rdf.createLiteral(n);
      addMap(modelBuilder, locations, name, v);
      return this;

    }

    public Builder addGoal(String name, int n) {
      Value v = RDFS.rdf.createLiteral(n);
      addMap(modelBuilder, goals, name, v);
      return this;
    }

    public MazeState build() {
      return new MazeState(locations, goals, modelBuilder.build());
    }
  }
}
