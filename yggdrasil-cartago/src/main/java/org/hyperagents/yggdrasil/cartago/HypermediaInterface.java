package org.hyperagents.yggdrasil.cartago;

import cartago.ArtifactDescriptor;
import cartago.ArtifactId;
import cartago.Workspace;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import ch.unisg.ics.interactions.wot.td.security.NoSecurityScheme;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class HypermediaInterface {
  private static final ValueFactory RDF = SimpleValueFactory.getInstance();

  private final Class<?> clazz;
  private final Workspace workspace;
  private final ArtifactId artifactId;
  private final List<ActionDescription> descriptions;
  private final Map<String, UnaryOperator<Object[]>> converterMap;
  private final Optional<String> name;
  private final Optional<String> hypermediaName;
  private final Set<String> feedbackActions;
  private final Map<String, UnaryOperator<Object>> responseConverterMap;
  private final Model metadata;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public HypermediaInterface(
      final Class<?> clazz,
      final Workspace workspace,
      final ArtifactId artifactId,
      final List<ActionDescription> descriptions,
      final Map<String, UnaryOperator<Object[]>> converterMap,
      final Optional<String> name,
      final Optional<String> hypermediaName,
      final Set<String> feedbackActions,
      final Map<String, UnaryOperator<Object>> responseConverterMap,
      final Model metadata
  ) {
    this.clazz = clazz;
    this.workspace = workspace;
    this.artifactId = artifactId;
    this.descriptions = new ArrayList<>(descriptions);
    this.converterMap = new HashMap<>(converterMap);
    this.name = name;
    this.hypermediaName = hypermediaName;
    this.feedbackActions = new HashSet<>(feedbackActions);
    this.responseConverterMap = new HashMap<>(responseConverterMap);
    this.metadata = metadata;
  }

  public HypermediaInterface(
      final Class<?> clazz,
      final Workspace workspace,
      final ArtifactId artifactId,
      final List<ActionDescription> descriptions,
      final Map<String, UnaryOperator<Object[]>> converterMap,
      final Optional<String> name,
      final Optional<String> hypermediaName,
      final Set<String> feedbackActions,
      final Map<String, UnaryOperator<Object>> responseConverterMap
  ) {
    this(
        clazz,
        workspace,
        artifactId,
        descriptions,
        converterMap,
        name,
        hypermediaName,
        feedbackActions,
        responseConverterMap,
        new LinkedHashModel()
    );
  }

  public ArtifactId getArtifactId() {
    return this.artifactId;
  }

  public String getArtifactName() {
    return this.name.orElseGet(this.artifactId::getName);
  }

  public String getActualArtifactName() {
    return this.artifactId.getName();
  }

  public String getHypermediaArtifactName() {
    return this.hypermediaName.orElseGet(this.artifactId::getName);
  }

  public String getArtifactUri() {
    return HypermediaArtifactRegistry.getInstance().getHttpWorkspacesPrefix()
           + this.workspace.getId().getName()
           + "/artifacts/"
           + this.getHypermediaArtifactName();
  }

  public String getHypermediaDescription() {
    final var tdBuilder =
        new ThingDescription.Builder(this.getArtifactName())
                            .addSecurityScheme(new NoSecurityScheme())
                            .addSemanticType("https://ci.mines-stetienne.fr/hmas/core#Artifact")
                            .addSemanticType(this.getSemanticType())
                            .addThingURI(this.getArtifactUri())
                            .addGraph(this.metadata);
    this.getActions()
        .values()
        .stream()
        .flatMap(List::stream)
        .forEach(tdBuilder::addAction);
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

  public Map<String, List<ActionAffordance>> getActions() {
    return this.descriptions
               .stream()
               .map(description -> {
                 final var actionName = description.getActionName();
                 final var actionBuilder =
                     new ActionAffordance.Builder(
                         actionName,
                         new Form.Builder(this.getArtifactUri() + description.getRelativeUri())
                                 .setMethodName(description.getMethodName())
                                 .build()
                     )
                     .addSemanticType(description.getActionClass())
                     .addTitle(actionName);
                 Optional.ofNullable(description.getInputSchema())
                         .ifPresent(actionBuilder::addInputSchema);
                 return Map.entry(actionName, List.of(actionBuilder.build()));
               })
               .collect(Collectors.toMap(
                 Map.Entry::getKey,
                 Map.Entry::getValue,
                 (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).toList()
               ));
  }

  public Set<String> getFeedbackActions() {
    return new HashSet<>(this.feedbackActions);
  }

  public Map<String, UnaryOperator<Object>> getResponseConverterMap() {
    return new HashMap<>(this.responseConverterMap);
  }

  private String getSemanticType() {
    return HypermediaArtifactRegistry
      .getInstance()
      .getArtifactSemanticType(this.clazz.getCanonicalName())
      .orElseThrow(() -> new NoSuchElementException("Artifact was not registered!"));
  }

  public Object[] convert(final String method, final Object... args) {
    return this.converterMap.containsKey(method)
           ? this.converterMap.get(method).apply(args)
           : args;
  }

  public static HypermediaInterface getBodyInterface(
      final Workspace workspace,
      final ArtifactDescriptor descriptor,
      final ArtifactId artifactId,
      final String agentIri
  ) {
    final var hypermediaArtifactName = HypermediaAgentBodyArtifactRegistry.getInstance().getName();
    final var artifactName = artifactId.getName();
    HypermediaAgentBodyArtifactRegistry.getInstance()
                                       .registerName(artifactName, hypermediaArtifactName);
    final var metadata = new LinkedHashModel();
    metadata.add(
        RDF.createIRI(HypermediaArtifactRegistry.getInstance().getHttpWorkspacesPrefix()
                      + workspace.getId().getName()
                      + "/artifacts/"
                      + hypermediaArtifactName),
        RDF.createIRI("https://purl.org/hmas/interaction#isAgentBodyOf"),
        RDF.createIRI(agentIri)
    );
    return new HypermediaInterface(
        descriptor.getArtifact().getClass(),
        workspace,
        artifactId,
        List.of(
          new ActionDescription.Builder(
            "focus",
            "http://example.org/focus",
            "/focus"
          )
          .setMethodName("PUT")
          .setInputSchema(new ArraySchema.Builder()
                                         .addItem(new StringSchema.Builder().build()).build())
          .build(),
          new ActionDescription.Builder(
            "focusWhenAvailable",
            "http://example.org/focusWhenAvailable",
            "/focusWhenAvailable"
          )
          .setMethodName("PUT")
          .setInputSchema(new ArraySchema.Builder()
                                         .addItem(new StringSchema.Builder().build()).build())
          .build(),
          new ActionDescription.Builder(
            "stopFocus",
            "http://example.org/stopFocus",
            "/stopFocus"
          )
          .setMethodName("DELETE")
          .setInputSchema(new ArraySchema.Builder()
                                         .addItem(new StringSchema.Builder().build())
                                         .build())
          .build()
        ),
        Map.of(
          "focus",
          args -> Stream.concat(
                          Arrays.stream(args)
                                .limit(1)
                                .map(e -> workspace.getArtifact(e.toString())),
                          Arrays.stream(args).skip(1).map(e -> null)
                        )
                        .toArray(),
          "stopFocus",
          args -> Arrays.stream(args)
                        .limit(1)
                        .map(e -> workspace.getArtifact(e.toString()))
                        .toArray()
        ),
        Optional.of(artifactName),
        Optional.of(hypermediaArtifactName),
        new HashSet<>(),
        new HashMap<>(),
        metadata
    );
  }
}
