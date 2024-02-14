package org.hyperagents.yggdrasil.utils.impl;

import ch.unisg.ics.interactions.hmas.core.hostables.Artifact;
import ch.unisg.ics.interactions.hmas.interaction.io.ArtifactProfileGraphWriter;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.ActionSpecification;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.ArtifactProfile;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.InputSpecification;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.Signifier;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import com.google.common.collect.ListMultimap;
import io.vertx.core.http.HttpMethod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.rdf4j.model.Model;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.RepresentationFactory;

public final class RepresentationFactoryImpl implements RepresentationFactory {
  private static final String ARTIFACT_NAME_PARAM = "artifactName";

  private final HttpInterfaceConfig httpConfig;

  public RepresentationFactoryImpl(final HttpInterfaceConfig httpConfig) {
    this.httpConfig = httpConfig;
  }

  @Override
  public String createPlatformRepresentation() {
    return serializeThingDescription(
      new ThingDescription
        .Builder("yggdrasil")
        .addThingURI(this.httpConfig.getBaseUri() + "/")
        .addSemanticType("https://purl.org/hmas/HypermediaMASPlatform")
        .addAction(
          new ActionAffordance.Builder(
              "createWorkspace",
              new Form.Builder(this.httpConfig.getWorkspacesUri() + "/")
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
                new Form.Builder(this.httpConfig.getArtifactsUri(workspaceName) + "/").build()
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
                new Form.Builder(this.httpConfig.getWorkspaceUri(workspaceName) + "/join")
                        .setMethodName(HttpMethod.POST.name())
                        .build()
            )
            .build()
          )
          .addAction(
            new ActionAffordance.Builder(
                "quitWorkspace",
                new Form.Builder(this.httpConfig.getWorkspaceUri(workspaceName) + "/leave")
                        .setMethodName(HttpMethod.POST.name())
                        .build()
            )
            .build()
          )
          .addAction(
            new ActionAffordance.Builder(
                "focus",
                new Form.Builder(this.httpConfig.getWorkspaceUri(workspaceName) + "/focus")
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
  public String createArtifactRepresentation(
      final String workspaceName,
      final String artifactName,
      final SecurityScheme securityScheme,
      final String semanticType,
      final Model metadata,
      final ListMultimap<String, ActionAffordance> actionAffordances
  ) {

    Set<Signifier> signifierList = new HashSet<>();

    for (ActionAffordance action : actionAffordances.values()) {
      Form formTD = action.getFirstForm().get();
      DataSchema inputSchema = action.getInputSchema().get();

      var form = new ch.unisg.ics.interactions.hmas.interaction.signifiers.Form.Builder(formTD.getTarget())
        .setMethodName(formTD.getMethodName().get())
        .setContentType(formTD.getContentType())
        .build();

      InputSpecification inputSpecification = new InputSpecification.Builder()
        .setRequiredSemanticTypes(inputSchema.getSemanticTypes())
        .setDataType(inputSchema.getDatatype())
        .build();

      ActionSpecification actionSpecification = new ActionSpecification.Builder(form)
        .addSemanticTypes((Set<String>) action.getSemanticTypes())
        .setRequiredInput(inputSpecification)
        .build();

      Signifier signifier = new Signifier.Builder(actionSpecification).build();

      signifierList.add(signifier);
    }


    Artifact artifact = new Artifact.Builder()
      .addSemanticType(semanticType)
      .setIRIAsString(this.httpConfig.getArtifactUri(workspaceName, artifactName))
      .build();

    ArtifactProfile artifactProfile = new ArtifactProfile.Builder(artifact)
      .setIRIAsString(this.httpConfig.getArtifactUri(workspaceName, artifactName))
      .exposeSignifiers(signifierList)
      .build();
    /*
    return serializeHmasArtifactProfile(artifactProfile);

     */

    final var td =
        new ThingDescription.Builder(artifactName)
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
      .write();
  }

  private String serializeHmasArtifactProfile(final ArtifactProfile profile) {
    return new ArtifactProfileGraphWriter(profile)
      .write();
  }
}
