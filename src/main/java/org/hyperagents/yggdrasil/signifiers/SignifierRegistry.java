package org.hyperagents.yggdrasil.signifiers;

import cartago.Tuple;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;



public class SignifierRegistry {

  private static SignifierRegistry registry;

  private String httpPrefix = "http://localhost:8080";

  // The IRI is the IRI of the signifier.
  //The tuple contains the name of the signifier at index 0.
  //The tuple contains the content of the signifier at index 1.
  //The tuple contains the visibility of the signifier at index 2.
  //The tuple contains the artifact of the signifier at index 3.
  private Map<IRI, Tuple> signifiers;

  private ValueFactory rdf;

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
    return this + "/signifiers/";
  }

  public IRI getSignifierIRI(String name){
    String iri = this + "/signifiers/" + name;
    return rdf.createIRI(iri);
  }

  public Set<IRI> getArtifactSignifiers(SignifierHypermediaArtifact artifact){
    Set<IRI> signifierIds = new HashSet<>();
    Set<IRI> keySet = this.signifiers.keySet();
    for (IRI key : keySet){
      Tuple t = signifiers.get(key);
      SignifierHypermediaArtifact a = (SignifierHypermediaArtifact) t.getContent(3);
      if (a.equals(artifact)){
        signifierIds.add(key);
      }
    }
    return signifierIds;

  }

  public void addSignifier(IRI name, Tuple t){
    this.signifiers.put(name, t);
  }

  public boolean isVisible(String agentName, String signifierUri){
    ValueFactory rdf = SimpleValueFactory.getInstance();
    IRI key = rdf.createIRI(signifierUri);
    boolean b = false;
    if (signifiers.containsKey(key)){
      Tuple t = signifiers.get(key);
      Signifier s = (Signifier) t.getContent(1);
      Visibility v = (Visibility) t.getContent(2);
      SignifierHypermediaArtifact artifact = (SignifierHypermediaArtifact) t.getContent(3);
      b = v.isVisible(artifact.getState(),artifact.getAgentProfile(agentName), s);

    }
    return b;

  }

  public String getSignifier(String agentUri, String signifier){
    String str = null;
    IRI signifierUri = getSignifierIRI(signifier);
    if (signifiers.containsKey(signifierUri)){
      Tuple t = signifiers.get(signifierUri);
      String signifierContent = (String) t.getContent(1);
      Visibility v = (Visibility) t.getContent(2);
      SignifierHypermediaArtifact artifact = (SignifierHypermediaArtifact) t.getContent(3);
      ArtifactState state = artifact.getState();
      AgentProfile profile = artifact.getAgentProfile(agentUri);
      Signifier s = new Signifier.Builder(getSignifierId(signifier))
        .add(contentToModel(signifierContent, signifierUri.toString()))
        .build();
      boolean b = v.isVisible(state, profile, s);
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
      Tuple t = signifiers.get(signifierId);
      s = (String) t.getContent(1);
    }
    return s;

  }
}
