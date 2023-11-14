package org.hyperagents.yggdrasil.cartago.entities;

import cartago.ArtifactDescriptor;
import cartago.ArtifactId;
import cartago.Workspace;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.hyperagents.yggdrasil.cartago.HypermediaAgentBodyArtifactRegistry;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifactRegistry;

public final class HypermediaInterfaceFactory {
  private HypermediaInterfaceFactory() {}

  public static HypermediaInterface createBodyInterface(
      final Workspace workspace,
      final ArtifactDescriptor descriptor,
      final ArtifactId artifactId,
      final String agentIri
  ) {
    final var rdf = SimpleValueFactory.getInstance();
    final var hypermediaArtifactName = HypermediaAgentBodyArtifactRegistry.getInstance().getName();
    final var artifactName = artifactId.getName();
    HypermediaAgentBodyArtifactRegistry.getInstance()
                                       .registerName(artifactName, hypermediaArtifactName);
    final var metadata = new LinkedHashModel();
    metadata.add(
        rdf.createIRI(HypermediaArtifactRegistry.getInstance().getHttpWorkspacesPrefix()
                      + workspace.getId().getName()
                      + "/artifacts/"
                      + hypermediaArtifactName),
        rdf.createIRI("https://purl.org/hmas/interaction#isAgentBodyOf"),
        rdf.createIRI(agentIri)
    );
    return new HypermediaInterface(
        descriptor.getArtifact().getClass(),
        workspace,
        artifactId,
        List.of(
          new ActionDescriptionFactory(
            "focus",
            "http://example.org/focus",
            "/focus"
          )
          .setMethodName("PUT")
          .setInputSchema(new ArraySchema.Builder()
                                         .addItem(new StringSchema.Builder().build()).build())
          .build(),
          new ActionDescriptionFactory(
            "focusWhenAvailable",
            "http://example.org/focusWhenAvailable",
            "/focusWhenAvailable"
          )
          .setMethodName("PUT")
          .setInputSchema(new ArraySchema.Builder()
                                         .addItem(new StringSchema.Builder().build()).build())
          .build(),
          new ActionDescriptionFactory(
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
