package org.hyperagents.yggdrasil.signifiers;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.hyperagents.io.SignifierWriter;
import org.hyperagents.ontologies.SignifierOntology;
import org.hyperagents.util.RDFS;
import org.hyperagents.util.ReifiedStatement;
import org.hyperagents.util.State;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

public class AgentProfile {

  private Resource agent;
  //private Model definition;
  //private Model comments;

  private Model model;

  public AgentProfile(Resource agent){
    this.agent = agent;
    //this.definition = new ModelBuilder().build();
    //this.comments = new ModelBuilder().build();
    this.model = new ModelBuilder().build();
    model.add(this.agent, RDF.TYPE, RDFS.rdf.createLiteral(AgentProfileOntology.Agent));
  }

  public AgentProfile(Resource agent, Model definition,Model comments){
    this.agent=agent;
    //this.definition=definition;
    //this.comments=comments;
    this.model = new ModelBuilder().build();
    model.add(this.agent, RDF.TYPE, RDFS.rdf.createLiteral(AgentProfileOntology.Agent));
    this.model.addAll(definition);
    this.model.addAll(comments);
  }

  public AgentProfile(Resource agent, Model model){
    this.agent = agent;
    this.model = model;
  }

  public Resource getAgent(){
    return this.agent;
  }

  /*public Model getDefinition(){
    return this.definition;
  }*/

  /*public Model getComments(){
    return this.comments;
  }*/

  public Model getComments(){
    return this.model;
  }

  public Model getModel1(){
    Model m =  new ModelBuilder().build();
    m.add(this.agent, RDF.TYPE, RDFS.rdf.createLiteral(AgentProfileOntology.Agent));
    //m.addAll(this.definition);
    //m.addAll(this.comments);
    return m;

  }

  public Model getModel(){
    return model;
  }

  public Optional<State> getPurpose(){
    Optional<State> opPurpose = Optional.empty();
    Optional<Resource> opStateId = Models.objectResource(model.filter(agent,
      RDFS.rdf.createIRI(AgentProfileOntology.hasPurpose), null));
    if (opStateId.isPresent()){
      Resource stateId = opStateId.get();
      State purpose = State.retrieveState(stateId, model);
      opPurpose = Optional.of(purpose);
    }
    return opPurpose;
  }

  public void rewrite(Model m){
    //this.comments = m;
    this.model = m;
  }

  public void add(Model m){
    //this.comments.addAll(m);
    this.model.addAll(m);
  }

  public void add(Resource subject, IRI predicate, Value object){
    this.model.add(subject, predicate, object);
  }

  public void add(Statement s){
    this.model.add(s.getSubject(), s.getPredicate(), s.getObject());
  }

  public void addToState(Resource stateId, ReifiedStatement statement){
    Resource statementId = statement.getStatementId();
    this.model.add(stateId, RDFS.rdf.createIRI(SignifierOntology.hasStatement), statementId);
    this.model.add(statementId, RDF.SUBJECT, statement.getSubject());
    this.model.add(statementId, RDF.PREDICATE, statement.getPredicate());
    this.model.add(statementId, RDF.OBJECT, statement.getObject());
  }

  public void addState(IRI predicate, State s){
    Resource stateId = s.getId();
    //this.comments.add(agent, predicate, stateId);
    //this.comments.addAll(s.getModel());
    this.model.add(agent, predicate, stateId);
    this.model.addAll(s.getModel());
  }

  public void delete(Statement s){
    this.model.remove(s.getSubject(), s.getPredicate(), s.getObject());
  }


  @Override
  public String toString(){
    return SignifierWriter.writeModel(this.getModel());
  }


  public static AgentProfile emptyProfile(Resource agent){
    return new AgentProfile(agent);
  }

  public static AgentProfile parse(String content){
    AgentProfile profile = null;
    Model model = retrieveModel(content,RDFFormat.TURTLE );
    Optional<Resource> optionalAgent = Models.subject(model.filter(null,
      RDF.TYPE, RDFS.rdf.createLiteral(AgentProfileOntology.Agent)));
    if (optionalAgent.isPresent()){
      Resource agent = optionalAgent.get();
      profile = new AgentProfile(agent, model);
    }
    return profile;

  }

  public static Model retrieveModel(String str, RDFFormat format){
    Model m = new ModelBuilder().build();
    RDFParser parser= Rio.createParser(format);
    parser.setRDFHandler(new StatementCollector(m));
    ByteArrayInputStream stream=new ByteArrayInputStream(str.getBytes());
    try{
      parser.parse(stream, "http://example.org/");
    }
    catch(IOException e){
      e.printStackTrace();
    }
    return m;
  }
}

