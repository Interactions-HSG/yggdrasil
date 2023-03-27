package org.hyperagents.yggdrasil.cartago;

import cartago.ArtifactDescriptor;
import cartago.ArtifactId;
import cartago.Workspace;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import ch.unisg.ics.interactions.wot.td.security.NoSecurityScheme;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.lang.reflect.Method;
import java.util.*;

public class HypermediaInterface {

  Class aClass;

  Workspace workspace;

  ArtifactId artifactId;

  List<ActionDescription> descriptions;

  Map<String, ArgumentConverter> converterMap;

  Optional<String> name;

  Optional<String> hypermediaName;

  private Set<String> feedbackActions = new HashSet<>();

  private Map<String, ResponseConverter> responseConverterMap = new Hashtable<>();

  private Model metadata;

  private static ValueFactory rdf = SimpleValueFactory.getInstance();

  public HypermediaInterface(Class aClass, Workspace workspace, ArtifactId artifactId, List<ActionDescription> descriptions, Map<String, ArgumentConverter> converterMap, Optional<String> name, Optional<String> hypermediaName, Set<String> feedbackActions, Map<String, ResponseConverter> responseConverterMap, Model metadata){
    this.aClass = aClass;
    this.workspace = workspace;
    this.artifactId = artifactId;
    this.descriptions = descriptions;
    this.converterMap = converterMap;
    this.name = name;
    this.hypermediaName = hypermediaName;
    this.feedbackActions = feedbackActions;
    this.responseConverterMap = responseConverterMap;
    this.metadata = metadata;
  }

  public HypermediaInterface(Class aClass, Workspace workspace, ArtifactId artifactId, List<ActionDescription> descriptions, Map<String, ArgumentConverter> converterMap, Optional<String> name, Optional<String> hypermediaName, Set<String> feedbackActions, Map<String, ResponseConverter> responseConverterMap){
    this.aClass = aClass;
    this.workspace = workspace;
    this.artifactId = artifactId;
    this.descriptions = descriptions;
    this.converterMap = converterMap;
    this.name = name;
    this.hypermediaName = hypermediaName;
    this.feedbackActions = feedbackActions;
    this.responseConverterMap = responseConverterMap;
    this.metadata = new LinkedHashModel();
  }

  public ArtifactId getArtifactId(){
    return artifactId;
  }


  public String getArtifactName() {
    if (name.isPresent()) {
      return name.get();
    } else {
      return artifactId.getName();
    }
  }

  public String getActualArtifactName(){
    return artifactId.getName();
  }

  public String getHypermediaArtifactName(){
    if (hypermediaName.isPresent()){
      return hypermediaName.get();
    }
    else {
      return artifactId.getName();
    }
  }

  public String getArtifactUri(){
    return HypermediaArtifactRegistry.getInstance().getHttpWorkspacesPrefix()+workspace.getId().getName()+"/artifacts/"+getHypermediaArtifactName();

  }

  public String getHypermediaDescription() {
    ThingDescription.Builder tdBuilder = new ThingDescription.Builder(getArtifactName())
      .addSecurityScheme(new NoSecurityScheme())
      .addSemanticType("https://ci.mines-stetienne.fr/hmas/core#Artifact")
      .addSemanticType(getSemanticType())
      .addThingURI(getArtifactUri())
      .addGraph(metadata);
    Map<String, List<ActionAffordance>> actionAffordances = getActions();

    for (String actionName : actionAffordances.keySet()) {
      for (ActionAffordance action : actionAffordances.get(actionName)){
        tdBuilder.addAction(action);
      }
    }

    return new TDGraphWriter(tdBuilder.build())
      .setNamespace("td", "https://www.w3.org/2019/wot/td#")
      .setNamespace("htv", "http://www.w3.org/2011/http#")
      .setNamespace("hctl", "https://www.w3.org/2019/wot/hypermedia#")
      .setNamespace("wotsec", "https://www.w3.org/2019/wot/security#")
      .setNamespace("dct", "http://purl.org/dc/terms/")
      .setNamespace("js", "https://www.w3.org/2019/wot/json-schema#")
      .setNamespace("eve", "http://w3id.org/eve#")
      .write();
  }

  public Map<String, ActionAffordance> getActions1(){
    Method[] methods = aClass.getMethods();
    Method[] actions = methods;
    Map<String, ActionAffordance> actionDescriptions = new Hashtable<>();
    for (Method action: actions){
      String name = action.getName();
      Form form = new Form.Builder(getArtifactUri()+"/"+name).build();
      ActionAffordance actionAffordance = new ActionAffordance.Builder(name, form)
        .build();
      actionDescriptions.put(name, actionAffordance);

    }
    return actionDescriptions;
  }

  public Map<String, List<ActionAffordance>> getActions(){
    Map<String, List<ActionAffordance>> actions = new Hashtable<>();
    for (ActionDescription description: descriptions){
      String actionClass = description.getActionClass();
      String actionName = description.getActionName();
      String relativeUri = description.getRelativeUri();
      String methodName = description.getMethodName();
      DataSchema inputSchema = description.getInputSchema();
      ActionAffordance.Builder actionBuilder = new ActionAffordance.Builder(actionName,
        new Form.Builder(getArtifactUri() + relativeUri)
          .setMethodName(methodName)
          .build())
        .addSemanticType(actionClass)
        .addTitle(actionName);

      if (inputSchema != null) {
        actionBuilder.addInputSchema(inputSchema);
      }
      if (actions.containsKey(actionName)){
        List<ActionAffordance> list = actions.get(actionName);
        list.add(actionBuilder.build());
        actions.put(actionName, list);
      } else {
        List<ActionAffordance> list = new ArrayList<>();
        list.add(actionBuilder.build());
        actions.put(actionName, list);

      }

    }
    return actions;

  }

  public Set<String> getFeedbackActions(){
    return feedbackActions;
  }

  public Map<String, ResponseConverter> getResponseConverterMap(){
    return responseConverterMap;
  }

  private String getSemanticType() {
    Optional<String> semType = HypermediaArtifactRegistry.getInstance().getArtifactSemanticType(
      this.aClass.getCanonicalName());

    if (semType.isPresent()) {
      return semType.get();
    }

    throw new RuntimeException("Artifact was not registered!");
  }

  public Object[] convert(String method, Object[] args){
    if (converterMap.containsKey(method)){
      ArgumentConverter converter = converterMap.get(method);
      return converter.convert(args);
    } else {
      return args;
    }
  }

  public static HypermediaInterface getBodyInterface(Workspace workspace, ArtifactDescriptor descriptor, ArtifactId artifactId, String agentIRI){
    Class aClass = descriptor.getArtifact().getClass();
    List<ActionDescription> descriptions = new ArrayList<>();
    ActionDescription focusDescription = new ActionDescription.Builder("focus", "http://example.org/focus", "/focus")
      .setMethodName("PUT")
      .setInputSchema(new ArraySchema.Builder()
        .addItem(new StringSchema.Builder().build())
        .build())
      .build();
    descriptions.add(focusDescription);
    ActionDescription focusWhenAvailableDescription = new ActionDescription.Builder("focusWhenAvailable", "http://example.org/focusWhenAvailable", "/focusWhenAvailable")
      .setMethodName("PUT")
      .setInputSchema(new ArraySchema.Builder()
        .addItem(new StringSchema.Builder().build())
        .build())
      .build();
    descriptions.add(focusWhenAvailableDescription);
    ActionDescription stopFocusDescription = new ActionDescription.Builder("stopFocus", "http://example.org/stopFocus", "/stopFocus")
      .setMethodName("DELETE")
      .setInputSchema(new ArraySchema.Builder()
        .addItem(new StringSchema.Builder().build())
        .build())
      .build();
    descriptions.add(stopFocusDescription);
    Map<String, ArgumentConverter> converters = new Hashtable<>();
    converters.put("focus", args -> {
      List<Object> objs = new ArrayList<>();
      int n = args.length;
      if (n>=1){
        ArtifactId id = workspace.getArtifact(args[0].toString());
        objs.add(id);
      }
      if (n>=2){
        for (int i = 1; i<n;i++){
          objs.add(null);
        }
      }
      return objs.toArray();
    });
    converters.put("stopFocus", args -> {
      List<Object> objs = new ArrayList<>();
      int n = args.length;
      if (n>=1){
        ArtifactId id = workspace.getArtifact(args[0].toString());
        objs.add(id);
      }
      return objs.toArray();
    });
    String hypermediaArtifactName = HypermediaAgentBodyArtifactRegistry.getInstance().getName();
    System.out.println("hypermedia artifact name");
    String artifactName = artifactId.getName();
    HypermediaAgentBodyArtifactRegistry.getInstance().registerName(artifactId.getName(), hypermediaArtifactName);
    Model metadata = new LinkedHashModel();
    String artifactUri = HypermediaArtifactRegistry.getInstance().getHttpWorkspacesPrefix()+workspace.getId().getName()+"/artifacts/"+hypermediaArtifactName;
    metadata.add(rdf.createIRI(artifactUri), rdf.createIRI("https://purl.org/hmas/interaction#isAgentBodyOf"), rdf.createIRI(agentIRI));
    return new HypermediaInterface(aClass, workspace, artifactId, descriptions,converters, Optional.of(artifactName), Optional.of(hypermediaArtifactName), new HashSet<>(), new Hashtable<>(), metadata);
  }

  public static HypermediaInterface getConsoleInterface(Workspace workspace, ArtifactDescriptor descriptor, ArtifactId artifactId){
    Class aClass = descriptor.getArtifact().getClass();
    List<ActionDescription> descriptions = new ArrayList<>();
    ActionDescription printDescription = new ActionDescription.Builder("print", "http://example.org/print","/print")
      .setInputSchema(new ArraySchema.Builder()
        .build()).build();
    ActionDescription printlnDescription = new ActionDescription.Builder("println", "http://example.org/println","/println")
      .setInputSchema(new ArraySchema.Builder().build()).build();
    ActionDescription printWithAgNameDescription = new ActionDescription.Builder("printWithAgName", "http://example.org/printWithAgName","/printWithAgName")
      .setInputSchema(new ArraySchema.Builder().build()).build();
    ActionDescription printlnWithAgNameDescription = new ActionDescription.Builder("printlnWithAgName", "http://example.org/printlnWithAgName","/printlnWithAgName")
      .setInputSchema(new ArraySchema.Builder().build()).build();
    descriptions.add(printDescription);
    descriptions.add(printlnDescription);
    descriptions.add(printWithAgNameDescription);
    descriptions.add(printlnWithAgNameDescription);
    Map<String, ArgumentConverter> converters = new Hashtable<>();
    return new HypermediaInterface(aClass, workspace, artifactId, descriptions,converters, Optional.empty(), Optional.empty(), new HashSet<>(), new Hashtable<>());
  }

}
