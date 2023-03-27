package org.hyperagents.yggdrasil.cartago;

import cartago.ArtifactDescriptor;
import cartago.ArtifactId;
import cartago.Workspace;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.util.*;

public class NativeHypermediaInterfaces {

  private static ValueFactory rdf = SimpleValueFactory.getInstance();

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

  //TODO: Check if useful and update
  public static HypermediaInterface getSystemInterface(Workspace workspace, ArtifactDescriptor descriptor, ArtifactId artifactId){
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


  //TODO: Update
  public static HypermediaInterface getManRepoInterface(Workspace workspace, ArtifactDescriptor descriptor, ArtifactId artifactId){
    Class aClass = descriptor.getArtifact().getClass();
    List<ActionDescription> descriptions = new ArrayList<>();
    ActionDescription storeManualDescription = new ActionDescription.Builder("storeManual", "http://example.org/storeManual","/storeManual")
      .setInputSchema(new ArraySchema.Builder()
        .addItem(new StringSchema.Builder().build())
        .addItem(new StringSchema.Builder().build())
        .build()).build();
    ActionDescription getManualContentDescription = new ActionDescription.Builder("getManualContent", "http://example.org/getManualContent","/getManualContent") //TODO: Update for return param
      .setInputSchema(new ArraySchema.Builder()
        .addItem(new StringSchema.Builder().build())
        .build())
      .build();
    descriptions.add(storeManualDescription);
    descriptions.add(getManualContentDescription);
    //TODO: add consult manual if needed
    Map<String, ArgumentConverter> converters = new Hashtable<>();
    return new HypermediaInterface(aClass, workspace, artifactId, descriptions,converters, Optional.empty(), Optional.empty(), new HashSet<>(), new Hashtable<>());
  }

  //TODO: See if needed and update
  public static HypermediaInterface getTupleSpaceInterface(Workspace workspace, ArtifactDescriptor descriptor, ArtifactId artifactId){
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
