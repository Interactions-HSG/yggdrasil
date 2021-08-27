package org.hyperagents.yggdrasil.signifiers;

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.hyperagents.util.RDFS;
import org.hyperagents.util.State;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifact;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AgentProfileArtifact extends HypermediaArtifact {

  private AgentProfile profile;

  private Optional<State> purpose;

  private Optional<State> currentSituation;



  public void init(Resource agent){
    profile = new AgentProfile(agent);
  }

  @OPERATION
  public void getAgentProfile(OpFeedbackParam<Object> returnParam){
    Resource agent = profile.getAgent();
    if (purpose.isPresent()){
      State purposeState = purpose.get();
      this.profile.addState(RDFS.rdf.createIRI(AgentProfileOntology.hasPurpose), purposeState);
    }
    if (currentSituation.isPresent()){
      State currentSituationState = currentSituation.get();
      this.profile.addState(RDFS.rdf.createIRI(AgentProfileOntology.hasCurrentSituation), currentSituationState);
    }
    returnParam.set(profile);

  }

  @OPERATION
  public void write_model_string(String s) {
    Model model = new ModelBuilder().build();
    RDFHandler handler = new StatementCollector(model);
    RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
    parser.setRDFHandler(handler);
    InputStream stream = new ByteArrayInputStream(s.getBytes());
    try {
      parser.parse(stream, "http://example.com/");
      rewrite(model);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @OPERATION
  public void write_string(String str){
    Statement s = getAsStatement(str);
    if (s!=null){
      Model model = new ModelBuilder().add(s.getSubject(), s.getPredicate(),s.getObject()).build();
      add(model);
    }

  }

  @OPERATION
  public void add(Model m){
    this.profile.add(m);
  }

  @OPERATION
  public void rewrite(Model m){
    this.profile.rewrite(m);
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
      registerActionAffordance("http://example.com/write", "rewrite", "/rewrite", new StringSchema.Builder().build());
      registerActionAffordance("http://example.com/writeString", "write_string", "/writestring", new StringSchema.Builder().build());
    }


}
