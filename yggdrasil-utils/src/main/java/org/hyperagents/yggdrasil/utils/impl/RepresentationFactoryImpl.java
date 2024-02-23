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

/**
 * This class is an implementation of the RepresentationFactory interface.
 * It provides methods to create representations of platforms, workspaces, artifacts, and bodies.
 * The representations are serialized as Thing Descriptions using the TDGraphWriter class.
 * The class also includes helper methods for serializing Thing Descriptions.
 */
public final class RepresentationFactoryImpl implements RepresentationFactory {

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

    Form createWorkspaceFormJson = new Form.Builder(baseUri + "/workspaces/")
      .setIRIAsString(baseUri + "/#createWorkspaceFormJson")
      .setMethodName(HttpMethod.POST.name())
      .build();

    Form createWorkspaceFormTxtTurtle = new Form.Builder(baseUri + "/workspaces/")
      .setIRIAsString(baseUri + "/#createWorkspaceFormTxtTurtle")
      .setMethodName(HttpMethod.POST.name())
      .setContentType("text/turtle")
      .build();

    Form registerToWebSubHub = new Form.Builder(baseUri + "/hub/")
      .setIRIAsString(baseUri + "/#registerToWebSubHub")
      .setMethodName(HttpMethod.POST.name())
      .setContentType("application/json")
      .build();

    Form sparqlGetQueryForm = new Form.Builder(baseUri + "/query/")
      .setIRIAsString(baseUri + "/#sparqlGetQueryForm")
      .setMethodName(HttpMethod.GET.name())
      .setContentType("application/sparql-query")
      .build();
    Form sparqlPostQueryForm = new Form.Builder(baseUri + "/query/")
      .setIRIAsString(baseUri + "/#sparqlPostQueryForm")
      .setMethodName(HttpMethod.POST.name())
      .setContentType("application/sparql-query")
      .build();

    ResourceProfile resourceProfile = new ResourceProfile.Builder(hypermediaMASPlatform)
      .setIRIAsString(baseUri + "/")
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(createWorkspaceFormJson)
            .build()
        ).build())
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(createWorkspaceFormTxtTurtle)
            .build()
        ).build())
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(registerToWebSubHub)
            .build()
        ).build())
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(sparqlGetQueryForm)
            .build()
        ).build())
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(sparqlPostQueryForm)
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
    Form makeArtifactForm = new Form.Builder(baseUri + "/artifacts/")
      .setMethodName(HttpMethod.POST.name())
      .setIRIAsString(baseUri + "#makeArtifactForm")
      .build();
    // TODO: Add inputSpecification to makeArtifact
    InputSpecification makeArtifactInputSpecification = new InputSpecification.Builder()
      .build();

    // join Workspace Signifier
    Form joinWorkspaceForm = new Form.Builder(baseUri + "/join/")
      .setMethodName(HttpMethod.POST.name())
      .setIRIAsString(baseUri + "#joinWorkspaceForm")
      .build();


    // leave Workspace Signifier
    Form leaveWorkspaceForm = new Form.Builder(baseUri + "/leave/")
      .setMethodName(HttpMethod.POST.name())
      .setIRIAsString(baseUri + "#leaveWorkspaceForm")
      .build();


    // focus Workspace Signifier
    Form focusWorkspaceForm = new Form.Builder(baseUri + "/focus/")
      .setMethodName(HttpMethod.POST.name())
      .setIRIAsString(baseUri + "#focusWorkspaceForm")
      .build();
    // TODO: Add inputSpecification to focus Workspace
    InputSpecification focusWorkspaceInputSpecification = new InputSpecification.Builder()
      .build();

    // create SubWorkspace Signifier
    Form createSubWorkspaceForm = new Form.Builder(baseUri + "/")
      .setMethodName(HttpMethod.POST.name())
      .setIRIAsString(baseUri + "#createSubWorkspaceForm")
      .build();

    // get current Workspace representation
    Form getCurrentWorkspaceForm = new Form.Builder(baseUri + "/")
      .setMethodName(HttpMethod.GET.name())
      .setIRIAsString(baseUri + "#getCurrentWorkspaceForm")
      .build();

    // update current workspace representation
    Form updateCurrentWorkspaceForm = new Form.Builder(baseUri + "/")
      .setMethodName(HttpMethod.PUT.name())
      .setIRIAsString(baseUri + "#updateCurrentWorkspaceForm")
      .build();

    // delete current workspace
    Form deleteCurrentWorkspaceForm = new Form.Builder(baseUri + "/")
      .setMethodName(HttpMethod.DELETE.name())
      .setIRIAsString(baseUri + "#deleteCurrentWorkspaceForm")
      .build();



    Signifier makeArtifactSignifier = new Signifier.Builder(new ActionSpecification.Builder(makeArtifactForm)
      .setRequiredInput(makeArtifactInputSpecification).build())
      .build();
    Signifier joinWorkspaceSignifier = new Signifier.Builder(new ActionSpecification.Builder(joinWorkspaceForm).build()).build();
    Signifier leaveWorkspaceSignifier = new Signifier.Builder(new ActionSpecification.Builder(leaveWorkspaceForm).build()).build();
    Signifier focusWorkspaceSignifier = new Signifier.Builder(new ActionSpecification.Builder(focusWorkspaceForm)
      .setRequiredInput(focusWorkspaceInputSpecification).build())
      .build();
    Signifier createSubWorkspaceSignifier = new Signifier.Builder(new ActionSpecification.Builder(createSubWorkspaceForm).build()).build();
    Signifier getCurrentWorkspaceSignifier = new Signifier.Builder(new ActionSpecification.Builder(getCurrentWorkspaceForm).build()).build();
    Signifier updateCurrentWorkspaceSignifier = new Signifier.Builder(new ActionSpecification.Builder(updateCurrentWorkspaceForm).build()).build();
    Signifier deleteCurrentWorkspaceSignifier = new Signifier.Builder(new ActionSpecification.Builder(deleteCurrentWorkspaceForm).build()).build();




    ResourceProfile resourceProfile = new ResourceProfile.Builder(workspace)
      .setIRIAsString(this.httpConfig.getWorkspaceUri(workspaceName))
      .exposeSignifier(makeArtifactSignifier)
      .exposeSignifier(joinWorkspaceSignifier)
      .exposeSignifier(leaveWorkspaceSignifier)
      .exposeSignifier(focusWorkspaceSignifier)
      .exposeSignifier(createSubWorkspaceSignifier)
      .exposeSignifier(getCurrentWorkspaceSignifier)
      .exposeSignifier(updateCurrentWorkspaceSignifier)
      .exposeSignifier(deleteCurrentWorkspaceSignifier)
      .build();

    return serializeHmasResourceProfile(resourceProfile);

  }

  @Override
  public String createArtifactRepresentation(
      final String workspaceName,
      final String artifactName,
      final String semanticType,
      final Model metadata,
      final ListMultimap<String, Signifier> signifiers
  ) {
    String baseUri = this.httpConfig.getArtifactUri(workspaceName, artifactName);

    Artifact artifact = new Artifact.Builder()
      .addSemanticType(semanticType)
      .setIRIAsString(baseUri+ "#artifact") //  #artifact
      .build();

    ResourceProfile.Builder resourceProfileBuilder = new ResourceProfile.Builder(artifact)
      .setIRIAsString(baseUri);
    signifiers.values().forEach(resourceProfileBuilder::exposeSignifier);

    // add Signifiers that are always given
    // get the representation for this artifact
    Form getArtifactRepresentationForm = new Form.Builder(baseUri)
      .setMethodName(HttpMethod.GET.name())
      .setIRIAsString(baseUri + "#getArtifactRepresentationForm")
      .build();

    // update this artifact
    Form updateArtifactForm = new Form.Builder(baseUri)
      .setMethodName(HttpMethod.PUT.name())
      .setIRIAsString(baseUri + "#updateArtifactForm")
      .build();

    // delete this artifact
    Form deleteArtifactForm = new Form.Builder(baseUri)
      .setMethodName(HttpMethod.DELETE.name())
      .setIRIAsString(baseUri + "#deleteArtifactForm")
      .build();

    Form doActionForm = new Form.Builder(baseUri)
      .setMethodName(HttpMethod.POST.name())
      .setIRIAsString(baseUri + "#doActionForm")
      .build();

    resourceProfileBuilder
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(getArtifactRepresentationForm)
            .build()
        ).build())
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(updateArtifactForm)
            .build()
        ).build())
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(deleteArtifactForm)
            .build()
        ).build())
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(doActionForm)
            .build()
        ).build());

    return serializeHmasResourceProfile(resourceProfileBuilder.build());
  }

  @Override
  public String createBodyRepresentation(
      final String workspaceName,
      final String agentName,
      final Model metadata
  ) {
    String baseUri = this.httpConfig.getAgentBodyUri(workspaceName, agentName);

    // TODO: isContainedIn should be directly on the artifact not the resourceProfile
    Agent agent = new Agent.Builder()
      .setIRIAsString(baseUri + "#agent")
      .addSemanticType("https://purl.org/hmas/Artifact")
      .addSemanticType("https://purl.org/hmas/agents-artifacts#Body")
      .build();

    ResourceProfile.Builder profile = new ResourceProfile.Builder(agent)
      .setIRIAsString(this.httpConfig.getAgentBodyUri(workspaceName, agentName));

    // Possible Signifiers of body
    Form getBodyRepresentationForm = new Form.Builder(baseUri)
      .setMethodName(HttpMethod.GET.name())
      .setIRIAsString(baseUri + "#getBodyRepresentationForm")
      .build();

    Form updateBodyForm = new Form.Builder(baseUri)
      .setMethodName(HttpMethod.PUT.name())
      .setIRIAsString(baseUri + "#updateBodyForm")
      .build();

    profile
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(getBodyRepresentationForm)
            .build()
        ).build())
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(updateBodyForm)
            .build()
        ).build());

    return serializeHmasResourceProfile(profile.build());
  }

  private String serializeHmasResourceProfile(final ResourceProfile profile) {
    return new ResourceProfileGraphWriter(profile)
      .setNamespace("hmas","https://purl.org/hmas/")
      .setNamespace("aa","https://purl.org/hmas/agents-artifacts#")
      .write();
  }
}
