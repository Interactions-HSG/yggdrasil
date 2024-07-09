package org.hyperagents.yggdrasil.utils.impl;

import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import ch.unisg.ics.interactions.wot.td.security.NoSecurityScheme;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import io.vertx.core.http.HttpMethod;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.RepresentationFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class RepresentationFactoryTDImplt implements RepresentationFactory {
  private static final String ARTIFACT_NAME_PARAM = "artifactName";

  private final HttpInterfaceConfig httpConfig;
  public enum WebSubMode {
    subscribe,
    unsubscribe
  }
  public RepresentationFactoryTDImplt(final HttpInterfaceConfig httpConfig) {
    this.httpConfig = httpConfig;
  }

  private ActionAffordance websubActions(final String actionName) {
    return new ActionAffordance.Builder(
      actionName,
      new Form.Builder(this.httpConfig.getBaseUri() + "hub/")
        .setMethodName(HttpMethod.POST.name())
        .setContentType("application/json")
        .build()
    ).addInputSchema(
      new ObjectSchema
        .Builder()
        .addProperty("callbackIri", new StringSchema.Builder().build())
        .addProperty("mode", new StringSchema.Builder().build())
        .addProperty("topic", new StringSchema.Builder().build())
        .build()
    ).addSemanticType("https://purl.org/hmas/jacamo/" + actionName)
      .build();
  }

  @Override
  public String createPlatformRepresentation() {
    return serializeThingDescription(
      new ThingDescription
        .Builder("Yggdrasil Node")
        .addThingURI(this.httpConfig.getBaseUri())
        .addSemanticType("https://purl.org/hmas/HypermediaMASPlatform")
        .addAction(
          new ActionAffordance.Builder(
            "createWorkspace",
            new Form.Builder(this.httpConfig.getWorkspacesUri())
              .setMethodName(HttpMethod.POST.name())
              .build()
          ).addSemanticType("https://purl.org/hmas/jacamo/CreateWorkspace")
           .build()
        )
        //.addAction(websubActions("subscribe"))
        //.addAction(websubActions("unsubscribe"))
    );
  }

  @Override
  public String createWorkspaceRepresentation(
    final String workspaceName,
    final Set<String> artifactTemplates
  ) {
    return serializeThingDescription(
      new ThingDescription
        .Builder(workspaceName)
        .addThingURI(this.httpConfig.getWorkspaceUri(workspaceName) + "#workspace")
        .addSemanticType("https://purl.org/hmas/Workspace")
        .addAction(
          new ActionAffordance.Builder(
            "makeArtifact",
            new Form.Builder(this.httpConfig.getArtifactsUri(workspaceName)).build()
          )
            .addInputSchema(
              new ObjectSchema
                .Builder()
                .addProperty(
                  "artifactClass",
                  new StringSchema.Builder().addEnum(artifactTemplates)
                    .addSemanticType("https://purl.org/hmas/jacamo/ArtifactTemplate")
                    .build()
                )
                .addProperty(ARTIFACT_NAME_PARAM, new StringSchema.Builder().addSemanticType("https://purl.org/hmas/jacamo/ArtifactName").build())
                .addProperty("initParams", new ArraySchema.Builder().addSemanticType("https://purl.org/hmas/jacamo/InitParams").build())
                .addRequiredProperties("artifactClass", ARTIFACT_NAME_PARAM)
                .build()
            ).addSemanticType("https://purl.org/hmas/jacamo/MakeArtifact")
            .build()
        )
        .addAction(
          new ActionAffordance.Builder(
            "joinWorkspace",
            new Form.Builder(this.httpConfig.getWorkspaceUri(workspaceName) + "join")
              .setMethodName(HttpMethod.POST.name())
              .build()
          ).addSemanticType("https://purl.org/hmas/jacamo/JoinWorkspace")
            .build()
        )
        .addAction(
          new ActionAffordance.Builder(
            "quitWorkspace",
            new Form.Builder(this.httpConfig.getWorkspaceUri(workspaceName) + "leave")
              .setMethodName(HttpMethod.POST.name())
              .build()
          ).addSemanticType("https://purl.org/hmas/jacamo/QuitWorkspace")
            .build()
        )
        .addAction(
          new ActionAffordance.Builder(
            "focus",
            new Form.Builder(this.httpConfig.getWorkspaceUri(workspaceName) + "focus")
              .setMethodName(HttpMethod.POST.name())
              .build()
          )
            .addInputSchema(
              new ObjectSchema
                .Builder()
                .addProperty(ARTIFACT_NAME_PARAM, new StringSchema.Builder().build())
                .addProperty("callbackIri", new StringSchema.Builder().build())
                .addRequiredProperties(ARTIFACT_NAME_PARAM, "callbackIri")
                .build()
            ).addSemanticType("https://purl.org/hmas/jacamo/Focus")
            .build()
        )
        .addAction(
          new ActionAffordance.Builder(
            "createSubWorkspace",
            new Form.Builder(this.httpConfig.getWorkspaceUri(workspaceName))
              .setMethodName(HttpMethod.POST.name())
              .build()
          ).addSemanticType("https://purl.org/hmas/jacamo/CreateSubWorkspace")
            .build()
        )
    );
  }

  @Override
  public String createArtifactRepresentation(final String workspaceName,final String artifactName,final String semanticType) {
    return createArtifactRepresentation(
      workspaceName,
      artifactName,
      new NoSecurityScheme(),
      semanticType,
      new LinkedHashModel(),
      Multimaps.newListMultimap(new HashMap<>(), ArrayList::new)
    );
  }
  @Override
  public String createArtifactRepresentation(final String workspaceName,final String artifactName,final String semanticType,final Model metadata,final ListMultimap<String, Object> actionAffordances) {
    return createArtifactRepresentation(
      workspaceName,
      artifactName,
      new NoSecurityScheme(),
      semanticType,
      metadata,
      actionAffordances
    );
  }


  @Override
  public String createArtifactRepresentation(
    final String workspaceName,
    final String artifactName,
    final SecurityScheme securityScheme,
    final String semanticType,
    final Model metadata,
    final ListMultimap<String, Object> actionAffordances
  ) {
    final ListMultimap<String, ActionAffordance> actionAffordancesMap = Multimaps.newListMultimap(new HashMap<>(), ArrayList::new);
    actionAffordances.entries().forEach(entry -> {
      final var actionName = entry.getKey();
      final var action = (ActionAffordance) entry.getValue();
      actionAffordancesMap.put(actionName, action);
    });
    final var td =
      new ThingDescription.Builder(artifactName)
        .addSecurityScheme(securityScheme)
        .addSemanticType("https://purl.org/hmas/Artifact")
        .addSemanticType(semanticType)
        .addThingURI(this.httpConfig
          .getArtifactUri(workspaceName, artifactName) + "#artifact")
        .addGraph(metadata);
    actionAffordancesMap.values().forEach(td::addAction);
    return serializeThingDescription(td);
  }

  @Override
  public String createBodyRepresentation(
    final String workspaceName,
    final String agentName,
    final Model metadata)
  {
    return createBodyRepresentation(workspaceName, agentName, new NoSecurityScheme(), metadata);
  }

  @Override
  public String createBodyRepresentation(
    final String workspaceName,
    final String agentName,
    final SecurityScheme securityScheme,
    final Model metadata
  ) {
    final var td =
      new ThingDescription
        .Builder(agentName)
        .addSecurityScheme(securityScheme)
        .addSemanticType("https://purl.org/hmas/Artifact")
        .addSemanticType("https://purl.org/hmas/jacamo/Body")
        .addThingURI(this.httpConfig.getAgentBodyUri(workspaceName, agentName) + "#artifact")
        .addGraph(metadata);
    return serializeThingDescription(td);
  }

  private String serializeThingDescription(final ThingDescription.Builder td) {
    return new TDGraphWriter(td.build())
      .setNamespace("td", "https://www.w3.org/2019/wot/td#")
      .setNamespace("htv", "http://www.w3.org/2011/http#")
      .setNamespace("hctl", "https://www.w3.org/2019/wot/hypermedia#")
      .setNamespace("wotsec", "https://www.w3.org/2019/wot/security#")
      .setNamespace("dct", "http://purl.org/dc/terms/")
      .setNamespace("js", "https://www.w3.org/2019/wot/json-schema#")
      .setNamespace("hmas", "https://purl.org/hmas/")
      .setNamespace("ex","http://example.org/")
      .setNamespace("jacamo", "https://purl.org/hmas/jacamo/")
      .write();
  }
}
