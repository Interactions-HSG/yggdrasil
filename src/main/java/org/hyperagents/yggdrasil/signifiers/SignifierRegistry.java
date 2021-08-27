package org.hyperagents.yggdrasil.signifiers;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.hyperagents.signifier.Signifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


public class SignifierRegistry {

  private static SignifierRegistry registry;

  private String httpPrefix = "http://localhost:8080";

  // The IRI is the IRI of the signifier.
  //The tuple contains the name of the signifier at index 0.
  //The tuple contains the content of the signifier at index 1.
  //The tuple contains the visibility of the signifier at index 2.
  //The tuple contains the artifact of the signifier at index 3.


  //The IRI is the IRI of the signifier
  //The tuple contains:
  //The content of the signifier at index 0
  //The visibility of the signifier at index 2.
  //The artifact of the signifier at index 3
  private Map<IRI, SignifierRegistryTuple> signifiers;

  private ValueFactory rdf;

  private int n = 1;

  private SignifierRegistry(){
    signifiers = new HashMap<>();
    rdf = SimpleValueFactory.getInstance();
  }

  public static synchronized SignifierRegistry getInstance(){
    if (registry == null) {
      registry = new SignifierRegistry();
    }

    return registry;
  }

  public String getSignifierPrefix(){
    return httpPrefix + "/signifiers/";
  }

  public IRI getSignifierIRI(String name){
    String iri = httpPrefix + "/signifiers/" + name;
    return rdf.createIRI(iri);
  }

  public Set<IRI> getArtifactSignifiers(SignifierHypermediaArtifact artifact){
    Set<IRI> signifierIds = new HashSet<>();
    Set<IRI> keySet = this.signifiers.keySet();
    for (IRI key : keySet){
      SignifierRegistryTuple t = signifiers.get(key);
      SignifierHypermediaArtifact a = t.getArtifact();
      if (a.equals(artifact)){
        signifierIds.add(key);
      }
    }
    return signifierIds;

  }

  public void addSignifier(IRI name, SignifierRegistryTuple t){
    this.signifiers.put(name, t);
  }

  public void addSignifier(SignifierRegistryTuple t){
    //IRI name = getSignifierIRI(getRandomName());
    IRI name = getSignifierIRI(createName());
    System.out.println("Signifier name: "+name);
    this.signifiers.put(name, t);
  }

  public String createName(){
    String s = ""+this.n;
    this.n++;
    return s;
  }

  public String getRandomName(){
    return UUID.randomUUID().toString();
  }

  public boolean isVisible(String agentName, String signifierUri){
    ValueFactory rdf = SimpleValueFactory.getInstance();
    IRI key = rdf.createIRI(signifierUri);
    boolean b = false;
    if (signifiers.containsKey(key)){
      SignifierRegistryTuple t = signifiers.get(key);
      Signifier s = t.getSignifier();
      Visibility v = t.getVisibility();
      SignifierHypermediaArtifact artifact = t.getArtifact();
      b = v.isVisible(s, artifact.getState(), artifact.getAgentProfile(agentName));

    }
    return b;

  }

  public String getSignifier1(String agentUri, String signifier){
    String str = null;
    IRI signifierUri = getSignifierIRI(signifier);
    if (signifiers.containsKey(signifierUri)){
      SignifierRegistryTuple t = signifiers.get(signifierUri);
      String signifierContent = t.getSignifierContent();
      Visibility v = t.getVisibility();
      SignifierHypermediaArtifact artifact = t.getArtifact();
      Model state = artifact.getState();
      AgentProfile profile = artifact.getAgentProfile(agentUri);
      Signifier s = new Signifier.Builder(getSignifierId(signifier))
        .add(contentToModel(signifierContent, signifierUri.toString()))
        .build();
      boolean b = v.isVisible(s, state, profile);
      if (b){
        str = signifierContent;
      }
    }
    return str;
  }

  public String getSignifier(String agentUri, String signifierName){
    String str = null;
    System.out.println(signifierName);
    IRI signifierUri = getSignifierIRI(signifierName);
    if (signifiers.containsKey(signifierUri)) {
      SignifierRegistryTuple t = signifiers.get(signifierUri);
      str = t.getSignifierContent();
    }
    return str;

  }

  public String getSignifierfromUri(String agentUri, String signifierUri){
    String str = null;
    if (signifiers.containsKey(signifierUri)){
      SignifierRegistryTuple t = signifiers.get(signifierUri);
      String signifierContent = t.getSignifierContent();
      Visibility v = t.getVisibility();
      SignifierHypermediaArtifact artifact = t.getArtifact();
      Model state = artifact.getState();
      AgentProfile profile = artifact.getAgentProfile(agentUri);
      Signifier s = new Signifier.Builder(rdf.createIRI(signifierUri))
        .add(contentToModel(signifierContent, signifierUri))
        .build();
      boolean b = v.isVisible(s, state, profile);
      if (b){
        str = signifierContent;
      }
    }
    return str;
  }

  private Model contentToModel(String content, String baseUri){
    Model model = new LinkedHashModel();
    InputStream stream = new ByteArrayInputStream(content.getBytes());
    RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
    parser.setRDFHandler(new StatementCollector(model));
    try {
      parser.parse(stream, baseUri);
    } catch(IOException e){
      e.printStackTrace();
    }
    return model;

  }

  private Resource getSignifierId(String name){
    return getSignifierIRI(name);
  }

  public String retrieveSignifier(IRI signifierId){
    String s = null;
    if (signifiers.containsKey(signifierId)){
      SignifierRegistryTuple t = signifiers.get(signifierId);
      s = t.getSignifierContent();
    }
    return s;

  }

  public List<IRI> getAllSignifierIRIs(){
    Set<IRI> iris = signifiers.keySet();
    return new ArrayList<>(iris);
  }
}
