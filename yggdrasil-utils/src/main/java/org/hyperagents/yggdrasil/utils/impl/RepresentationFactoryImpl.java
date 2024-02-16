package org.hyperagents.yggdrasil.utils.impl;

import ch.unisg.ics.interactions.hmas.core.hostables.Agent;
import ch.unisg.ics.interactions.hmas.core.hostables.Artifact;
import ch.unisg.ics.interactions.hmas.core.hostables.HypermediaMASPlatform;
import ch.unisg.ics.interactions.hmas.core.hostables.Workspace;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphWriter;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.ActionSpecification;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.InputSpecification;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.ResourceProfile;
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


import java.util.Arrays;
import java.util.HashSet;
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
    String baseUri = this.httpConfig.getBaseUri();
    HypermediaMASPlatform hypermediaMASPlatform = new HypermediaMASPlatform.Builder()
      .setIRIAsString(baseUri + "/#platform")
      .addSemanticType("https://purl.org/hmas/HypermediaMASPlatform")
      .build();

    var form = new ch.unisg.ics.interactions.hmas.interaction.signifiers.Form.Builder(baseUri + "/workspaces/")
      .setIRIAsString(baseUri + "/#form")
      .setMethodName(HttpMethod.POST.name())
      .build();

    ResourceProfile resourceProfile = new ResourceProfile.Builder(hypermediaMASPlatform)
      .setIRIAsString(baseUri + "/")
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(form)
            .build()
        ).build())
      .build();

    return serializeHmasArtifactProfile(resourceProfile);
      /*
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

       */
  }

  @Override
  public String createWorkspaceRepresentation(
      final String workspaceName,
      final Set<String> artifactTemplates
  ) {

    Workspace workspace = new Workspace.Builder()
      .setIRIAsString(this.httpConfig.getWorkspaceUri(workspaceName) + "#workspace")
      .addSemanticType("https://purl.org/hmas/Workspace")
      .build();

    ResourceProfile resourceProfile = new ResourceProfile.Builder(workspace)
      .setIRIAsString(this.httpConfig.getWorkspaceUri(workspaceName))
      .build();

    return serializeHmasArtifactProfile(resourceProfile);
    /*
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
     */
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
    Form formTD = null;
    DataSchema inputSchema = null;

    for (ActionAffordance action : actionAffordances.values()) {
      try {
        formTD = action.getFirstForm().get();
      } catch (Exception e) {
        System.out.println("failed to get firstForm of " + action.getName());
      }
      try {
        inputSchema = action.getInputSchema().get();
      } catch (Exception e) {
        System.out.println("failed to get inputSchema of " + action.getName());
      }

      ch.unisg.ics.interactions.hmas.interaction.signifiers.Form form = null;
      try {

        form = new ch.unisg.ics.interactions.hmas.interaction.signifiers.Form.Builder(formTD.getTarget())
          .setIRIAsString(this.httpConfig.getArtifactUri(workspaceName, artifactName) + "#form") // #form
          .setMethodName(formTD.getMethodName().get())
          .setContentType(formTD.getContentType())
          .build();
      } catch (Exception e) {
        System.out.println("failed to create form");
      }

      InputSpecification inputSpecification = null;

      try {
        inputSpecification = new InputSpecification.Builder()
          .build();
      } catch (Exception e) {
        System.out.println("Failed to get semantic types from inputSchema");
      }

      ActionSpecification actionSpecification = null;

      try {
        actionSpecification = new ActionSpecification.Builder(form)
          .build();
      } catch (Exception e ) {
        System.out.println("failed to get semantic types from action");
      }
      Signifier signifier = new Signifier.Builder(actionSpecification)
        .build();

      signifierList.add(signifier);
    }


    Artifact artifact = new Artifact.Builder()
      .addSemanticType(semanticType)
      .setIRIAsString(this.httpConfig.getArtifactUri(workspaceName, artifactName) + "#artifact") //  #artifact
      .build();

    ResourceProfile artifactProfile = new ResourceProfile.Builder(artifact)
      .setIRIAsString(this.httpConfig.getArtifactUri(workspaceName, artifactName))
      .exposeSignifiers(signifierList)
      .build();

    return serializeHmasArtifactProfile(artifactProfile);
    /*

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
     */
  }

  @Override
  public String createBodyRepresentation(
      final String workspaceName,
      final String agentName,
      final SecurityScheme securityScheme,
      final Model metadata
  ) {
    Agent agent = new Agent.Builder()
      .setIRIAsString(this.httpConfig.getAgentBodyUri(workspaceName, agentName) + "#agent")
      .addSemanticType("https://purl.org/hmas/Artifact")
      .addSemanticType("https://example.org/Body")
      .build();

    ResourceProfile profile = new ResourceProfile.Builder(agent)
      .setIRIAsString(this.httpConfig.getAgentBodyUri(workspaceName, agentName))
      .build();

    return serializeHmasArtifactProfile(profile);
    /*
    final var td =
        new ThingDescription
          .Builder(agentName)
          .addSecurityScheme(securityScheme)
          .addSemanticType("https://purl.org/hmas/Artifact")
          .addSemanticType("https://example.org/Body")
          .addThingURI(this.httpConfig.getAgentBodyUri(workspaceName, agentName))
          .addGraph(metadata);
    return serializeThingDescription(td);
     */
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

  private String serializeHmasArtifactProfile(final ResourceProfile profile) {
    return new ResourceProfileGraphWriter(profile)
      .write();
  }
}
