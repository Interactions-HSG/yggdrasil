package org.hyperagents.yggdrasil.utils.impl;

import ch.unisg.ics.interactions.hmas.interaction.signifiers.Signifier;
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
import io.vertx.core.http.HttpMethod;
import org.eclipse.rdf4j.model.Model;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.RepresentationFactory;

import java.util.Set;

public class RepresentationFactoryTDImplt implements RepresentationFactory {
  private static final String ARTIFACT_NAME_PARAM = "artifactName";

  private final HttpInterfaceConfig httpConfig;

  public RepresentationFactoryTDImplt(final HttpInterfaceConfig httpConfig) {
    this.httpConfig = httpConfig;
  }

  @Override
  public String createPlatformRepresentation() {
    return serializeThingDescription(
      new ThingDescription
        .Builder("yggdrasil")
        .addThingURI(this.httpConfig.getBaseUri())
        .addSemanticType("https://purl.org/hmas/HypermediaMASPlatform")
        .addAction(
          new ActionAffordance.Builder(
            "createWorkspace",
            new Form.Builder(this.httpConfig.getWorkspacesUri())
              .setMethodName(HttpMethod.POST.name())
              .build()
          )
            .build()
        )
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
        .addThingURI(this.httpConfig.getWorkspaceUri(workspaceName))
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
                  new StringSchema.Builder().addEnum(artifactTemplates).build()
                )
                .addProperty(ARTIFACT_NAME_PARAM, new StringSchema.Builder().build())
                .addProperty("initParams", new ArraySchema.Builder().build())
                .addRequiredProperties("artifactClass", ARTIFACT_NAME_PARAM)
                .build()
            )
            .build()
        )
        .addAction(
          new ActionAffordance.Builder(
            "joinWorkspace",
            new Form.Builder(this.httpConfig.getWorkspaceUri(workspaceName) + "join")
              .setMethodName(HttpMethod.POST.name())
              .build()
          )
            .build()
        )
        .addAction(
          new ActionAffordance.Builder(
            "quitWorkspace",
            new Form.Builder(this.httpConfig.getWorkspaceUri(workspaceName) + "leave")
              .setMethodName(HttpMethod.POST.name())
              .build()
          )
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
            )
            .build()
        )
        .addAction(
          new ActionAffordance.Builder(
            "createSubWorkspace",
            new Form.Builder(this.httpConfig.getWorkspaceUri(workspaceName))
              .setMethodName(HttpMethod.POST.name())
              .build()
          )
            .build()
        )
    );
  }

  @Override
  public String createArtifactRepresentation(String workspaceName, String artifactName, String semanticType, Model metadata, ListMultimap<String, Signifier> actionAffordances) {
    return null;
  }

  @Override
  public String createArtifactRepresentation(
    final String workspaceName,
    final String artifactName,
    final SecurityScheme securityScheme,
    final String semanticType,
    final Model metadata,
    final ListMultimap<String, ActionAffordance> actionAffordances
  ) {
    final var td =
      new ThingDescription.Builder(artifactName + "#artifact")
        .addSecurityScheme(securityScheme)
        .addSemanticType("https://purl.org/hmas/Artifact")
        .addSemanticType(semanticType)
        .addThingURI(this.httpConfig
          .getArtifactUri(workspaceName, artifactName))
        .addGraph(metadata);
    actionAffordances.values().forEach(td::addAction);
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
        .addSemanticType("https://example.org/Body")
        .addThingURI(this.httpConfig.getAgentBodyUri(workspaceName, agentName))
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
      .write();
  }
}
