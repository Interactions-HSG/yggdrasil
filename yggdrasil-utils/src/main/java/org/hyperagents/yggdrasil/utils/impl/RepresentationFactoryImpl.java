package org.hyperagents.yggdrasil.utils.impl;

import ch.unisg.ics.interactions.hmas.core.hostables.Agent;
import ch.unisg.ics.interactions.hmas.core.hostables.Artifact;
import ch.unisg.ics.interactions.hmas.core.hostables.HypermediaMASPlatform;
import ch.unisg.ics.interactions.hmas.core.hostables.Workspace;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphWriter;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.*;
import com.google.common.collect.ListMultimap;
import io.vertx.core.http.HttpMethod;


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

    Form form = new Form.Builder(baseUri + "/workspaces/")
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

    return serializeHmasResourceProfile(resourceProfile);
  }

  @Override
  public String createWorkspaceRepresentation(
      final String workspaceName,
      final Set<String> artifactTemplates
  ) {
    String baseUri = this.httpConfig.getWorkspaceUri(workspaceName);
    Workspace workspace = new Workspace.Builder()
      .setIRIAsString(baseUri + "#workspace")
      .addSemanticType("https://purl.org/hmas/Workspace")
      .build();

    // makeArtifact Signifier
    Form makeArtifactForm = new Form.Builder(this.httpConfig.getArtifactsUri(workspaceName) + "/")
      .setMethodName(HttpMethod.POST.name())
      .setIRIAsString(baseUri + "#makeArtifact")
      .build();

    // TODO: Add inputSpecification to makeArtifact
    InputSpecification makeArtifactInputSpecification = new InputSpecification.Builder()
      .build();

    Signifier makeArtifactSignifier = new Signifier.Builder(new ActionSpecification.Builder(makeArtifactForm)
      .setRequiredInput(makeArtifactInputSpecification).build())
      .build();

    /*
    addInputSchema(
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
     */


    // join Workspace Signifier
    Form joinWorkspaceForm = new Form.Builder(this.httpConfig.getWorkspaceUri(workspaceName) + "/join")
      .setMethodName(HttpMethod.POST.name())
      .setIRIAsString(baseUri + "#joinWorkspace")
      .build();
    Signifier joinWorkspaceSignifier = new Signifier.Builder(new ActionSpecification.Builder(joinWorkspaceForm).build()).build();

    // leave Workspace Signifier
    Form leaveWorkspaceForm = new Form.Builder(this.httpConfig.getWorkspaceUri(workspaceName) + "/leave")
      .setMethodName(HttpMethod.POST.name())
      .setIRIAsString(baseUri + "#leaveWorkspace")
      .build();
    Signifier leaveWorkspaceSignifier = new Signifier.Builder(new ActionSpecification.Builder(leaveWorkspaceForm).build()).build();

    // focus Workspace Signifier
    Form focusWorkspaceForm = new Form.Builder(this.httpConfig.getWorkspaceUri(workspaceName) + "/focus")
      .setMethodName(HttpMethod.POST.name())
      .setIRIAsString(baseUri + "#focusWorkspace")
      .build();

    // TODO: Add inputSpecification to focus Workspace
    InputSpecification focusWorkspaceInputSpecification = new InputSpecification.Builder()
      .build();

    /*
   .addInputSchema(
      new ObjectSchema
        .Builder()
        .addProperty(ARTIFACT_NAME_PARAM, new StringSchema.Builder().build())
        .addProperty("callbackIri", new StringSchema.Builder().build())
        .addRequiredProperties(ARTIFACT_NAME_PARAM, "callbackIri")
        .build()
    )
    .build()
     */

    Signifier focusWorkspaceSignifier = new Signifier.Builder(new ActionSpecification.Builder(focusWorkspaceForm)
      .setRequiredInput(focusWorkspaceInputSpecification).build())
      .build();

    // create SubWorkspace Signifier
    Form createSubWorkspaceForm = new Form.Builder(this.httpConfig.getWorkspaceUri(workspaceName))
      .setMethodName(HttpMethod.POST.name())
      .setIRIAsString(baseUri + "#createSubWorkspace")
      .build();
    Signifier createSubWorkspaceSignifier = new Signifier.Builder(new ActionSpecification.Builder(createSubWorkspaceForm).build()).build();


    ResourceProfile resourceProfile = new ResourceProfile.Builder(workspace)
      .setIRIAsString(this.httpConfig.getWorkspaceUri(workspaceName))
      .exposeSignifier(makeArtifactSignifier)
      .exposeSignifier(joinWorkspaceSignifier)
      .exposeSignifier(leaveWorkspaceSignifier)
      .exposeSignifier(focusWorkspaceSignifier)
      .exposeSignifier(createSubWorkspaceSignifier)
      .build();

    return serializeHmasResourceProfile(resourceProfile);

  }

  @Override
  public String createArtifactRepresentation(
      final String workspaceName,
      final String artifactName,
      final String semanticType,
      final Model metadata,
      final ListMultimap<String, Signifier> actionAffordances
  ) {
    Artifact artifact = new Artifact.Builder()
      .addSemanticType(semanticType)
      .setIRIAsString(this.httpConfig.getArtifactUri(workspaceName, artifactName) + "#artifact") //  #artifact
      .build();

    ResourceProfile.Builder resourceProfileBuilder = new ResourceProfile.Builder(artifact)
      .setIRIAsString(this.httpConfig.getArtifactUri(workspaceName, artifactName));
    actionAffordances.values().forEach(resourceProfileBuilder::exposeSignifier);

    return serializeHmasResourceProfile(resourceProfileBuilder.build());
  }

  @Override
  public String createBodyRepresentation(
      final String workspaceName,
      final String agentName,
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

    return serializeHmasResourceProfile(profile);
  }

  private String serializeHmasResourceProfile(final ResourceProfile profile) {
    return new ResourceProfileGraphWriter(profile)
      .setNamespace("hmas","https://purl.org/hmas/")
      .write();
  }
}
