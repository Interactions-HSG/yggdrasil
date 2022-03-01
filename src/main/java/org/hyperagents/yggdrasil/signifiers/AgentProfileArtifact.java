package org.hyperagents.yggdrasil.signifiers;

import cartago.*;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.schemas.IntegerSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.hyperagents.util.RDFS;
import org.hyperagents.util.ReifiedStatement;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifact;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AgentProfileArtifact extends HypermediaArtifact {


  private String creatorAgent;

  private AgentProfile profile;


  public void init(String agentName){
    Resource agent = RDFS.rdf.createIRI(agentName);
    profile = new AgentProfile(agent);
    this.creatorAgent = agentName;
  }


  @OPERATION
  public void getAgentProfile(OpFeedbackParam<Object> returnParam){
    String profileString = profile.toString();
    returnParam.set(profileString);
  }

  @LINK
  public void retrieveAgentProfile(OpFeedbackParam<AgentProfile> returnParam){
    returnParam.set(profile);
  }

  @OPERATION
  public void write_model_string(String s) {
    String agentName = this.getCurrentOpAgentId().getAgentName();
    await("isCreatorAgent", agentName);
    Model model = new ModelBuilder().build();
    RDFHandler handler = new StatementCollector(model);
    RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
    parser.setRDFHandler(handler);
    InputStream stream = new ByteArrayInputStream(s.getBytes());
    try {
      parser.parse(stream, "http://example.org/");
      rewrite(model);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @OPERATION
  public void write_string(String str){
    String agentName = this.getCurrentOpAgentId().getAgentName();
    await("isCreatorAgent", agentName);
    Statement s = getAsStatement(str);
    if (s!=null){
      Model model = new ModelBuilder().add(s.getSubject(), s.getPredicate(),s.getObject()).build();
      add(model);
    }

  }


  public void add(Model m){
    this.profile.add(m);
  }


  public void rewrite(Model m){
    this.profile.rewrite(m);
  }


  @OPERATION
  public void addPurpose(String str){
    String agentName = this.getCurrentOpAgentId().getAgentName();
    await("isCreatorAgent", agentName);
    Statement s = getAsStatement(str);
    ReifiedStatement rs = getAsReifiedStatement(s);
    Optional<Resource> opStateId = Models.objectResource(profile.getModel().filter(profile.getAgent(),
      RDFS.rdf.createIRI(AgentProfileOntology.hasPurpose), null));
    if (s!=null){
      if (opStateId.isPresent()){
        Resource stateId = opStateId.get();
        this.profile.addToState(stateId, rs);
      }
      else {
        Resource stateId = RDFS.rdf.createBNode();
        this.profile.add(profile.getAgent(), RDFS.rdf.createIRI(AgentProfileOntology.hasPurpose), stateId);
        this.profile.addToState(stateId, rs);
      }
    }
  }

  @OPERATION
  public void addSituation(String str){
    String agentName = this.getCurrentOpAgentId().getAgentName();
    await("isCreatorAgent", agentName);
    Statement s = getAsStatement(str);
    ReifiedStatement rs = getAsReifiedStatement(s);
    Optional<Resource> opStateId = Models.objectResource(profile.getModel().filter(profile.getAgent(),
      RDFS.rdf.createIRI(AgentProfileOntology.hasCurrentSituation), null));
    if (s!=null){
      if (opStateId.isPresent()){
        Resource stateId = opStateId.get();
        this.profile.addToState(stateId, rs);
      }
      else {
        Resource stateId = RDFS.rdf.createBNode();
        this.profile.add(profile.getAgent(), RDFS.rdf.createIRI(AgentProfileOntology.hasCurrentSituation), stateId);
        this.profile.addToState(stateId, rs);
      }
    }
  }

  @OPERATION
  public void addMaxSignifiers(int maxSignifiers){
    System.out.println("add max signifiers: ");
    System.out.println(maxSignifiers);
    String agentName = this.getCurrentOpAgentId().getAgentName();
    await("isCreatorAgent", agentName);
    Statement s = RDFS.rdf.createStatement(RDFS.rdf.createIRI(AgentProfileOntology.thisAgent), RDFS.rdf.createIRI(AgentProfileOntology.maxSignifiers), RDFS.rdf.createLiteral(maxSignifiers));
    ReifiedStatement rs = getAsReifiedStatement(s);
    Optional<Literal> opMax = Models.objectLiteral(profile.getModel().filter(profile.getAgent(),
      RDFS.rdf.createIRI(AgentProfileOntology.maxSignifiers), null));
    if (s!=null){
      if (opMax.isPresent()){
        this.profile.getModel().remove(profile.getAgent(), RDFS.rdf.createIRI(AgentProfileOntology.maxSignifiers), opMax.get());
        this.profile.add(s);

      } else {
        this.profile.add(s);
      }

    }

  }

  /*@GUARD
  public boolean isCreatorAgent(){
    boolean b = false;
    AgentId currentAgent = this.getCurrentOpAgentId();
    AgentId creatorAgent = this.creatorAgent;
    if (currentAgent.equals(creatorAgent)){
      b = true;
    }
    return b;
  }*/

  @GUARD
   boolean isCreatorAgent(String agentName){
    return agentName.equals(creatorAgent);
  }

  private ReifiedStatement getAsReifiedStatement(Statement s) {
    Resource statementId = RDFS.rdf.createBNode();
    ReifiedStatement rs = new ReifiedStatement(statementId, s.getSubject(), s.getPredicate(), s.getObject());
    return rs;
  }

  public Statement getAsStatement(String str) {
    Pattern tripleTermPattern = Pattern.compile("rdf\\((.*),(.*),(.*)\\)");
    Matcher m = tripleTermPattern.matcher(str);
    if (m.matches() && !hasObsPropertyByTemplate("rdf", m.group(1), m.group(2), m.group(3))) {
      String subj = removeQuotes(m.group(1));
      String pred = removeQuotes(m.group(2));
      String obj = removeFirstQuotes(m.group(3));
      Resource subject = getResource(subj);
      IRI predicate = getIRI(pred);
      Value object = getValue(obj);
      Statement s = RDFS.rdf.createStatement(subject, predicate, object);
      return s;
    }
    return null;
  }

  private boolean isIRI(String str){
    boolean b = str.startsWith("http");
    return b;
  }

  private IRI getIRI(String str){
    return RDFS.rdf.createIRI(str);
  }

  private boolean isBNode(String str){
    boolean b = str.startsWith("_:");
    return b;
  }

  private String toBNode(String str){
    String s = null;
    if (isBNode(str)){
      s = str.substring(2);
    }
    return s;

  }

  private BNode getBNode(String str){
    return RDFS.rdf.createBNode(toBNode(str));
  }

  private Resource getResource(String str){
    if (isIRI(str)){
      return getIRI(str);
    }
    else if (isBNode(str)){
      return getBNode(str);
    }
    else {
      return RDFS.rdf.createBNode();
    }

  }

  private Literal getLiteral(String str){
    Literal l = RDFS.rdf.createLiteral(str);
    return RDFS.rdf.createLiteral(str);
  }

  private Value getValue(String str){
    boolean isIri = isIRI(str);
    if (isIRI(str)){
      return getIRI(str);
    }
    else if (isBNode(str)){
      return getBNode(str);
    }
    else {
      return getLiteral(str);
    }

  }

    private String removeQuotes(String str){
      String s = "";
      int length = str.length();
      for (int i = 0; i<length; i++){
        if (str.charAt(i) != '"'){
          s = s +str.charAt(i);
        }
      }
      return s;
    }



    private String removeFirstQuotes(String str) {
      String s = "";
      int length = str.length();
      for (int i = 0; i < length; i++) {
        char c = str.charAt(i);
        if (!(c == '"' && (i == 0 || i == length - 1))) {
          s = s + c;
        }
      }
      return s;
    }


    @Override
  protected void registerInteractionAffordances() {
      registerActionAffordance("http://example.org/write", "rewrite", "/rewrite", new StringSchema.Builder().build());
      registerActionAffordance("http://example.org/writeString", "write_string", "/writestring", new StringSchema.Builder().build());
      DataSchema stateSchema = new ArraySchema.Builder()
        .addItem(new StringSchema.Builder().build())
        .build();

      registerActionAffordance("http://example.org/addPurpose", "addPurpose", "/purpose", stateSchema);
      registerActionAffordance("http://example.org/addSituation", "addSituation", "/situation", stateSchema);
      registerActionAffordance("http://example.org/getAgentProfile", "getAgentProfile", "/profile");
      DataSchema maxSignifiersSchema = new ArraySchema.Builder()
        .addItem(new IntegerSchema.Builder().build())
        .build();
      registerActionAffordance("http://example.org/maxSignifiers", "addMaxSignifiers","/max", maxSignifiersSchema);
    }


}
