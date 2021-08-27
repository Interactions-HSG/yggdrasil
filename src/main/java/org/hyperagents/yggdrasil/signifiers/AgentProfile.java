package org.hyperagents.yggdrasil.signifiers;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.hyperagents.util.RDFS;
import org.hyperagents.util.State;

public class AgentProfile {

  private Resource agent;
  private Model definition;
  private Model comments;

  public AgentProfile(Resource agent){
    this.agent = agent;
    this.definition = new ModelBuilder().build();
    this.comments = new ModelBuilder().build();
  }

  public AgentProfile(Resource agent, Model definition,Model comments){
    this.agent=agent;
    this.definition=definition;
    this.comments=comments;
  }

  public Resource getAgent(){
    return this.agent;
  }

  public Model getDefinition(){
    return this.definition;
  }

  public Model getComments(){
    return this.comments;
  }

  public Model getModel(){
    Model m =  new ModelBuilder().build();
    m.add(this.agent, RDF.TYPE, RDFS.rdf.createLiteral(AgentProfileOntology.Agent));
    m.addAll(this.definition);
    m.addAll(this.comments);
    return m;

  }

  public void rewrite(Model m){
    this.comments = m;
  }

  public void add(Model m){
    this.comments.addAll(m);
  }

  public void addState(IRI predicate, State s){
    Resource stateId = s.getStateId();
    this.comments.add(agent, predicate, stateId);
    this.comments.addAll(s.getModel());
  }

  public void delete(Statement s){
    this.comments.remove(s.getSubject(), s.getPredicate(), s.getObject());
  }



  public static AgentProfile emptyProfile(Resource agent){
    return new AgentProfile(agent);


  }
}

