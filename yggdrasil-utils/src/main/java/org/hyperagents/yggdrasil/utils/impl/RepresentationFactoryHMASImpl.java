package org.hyperagents.yggdrasil.utils.impl;


import ch.unisg.ics.interactions.hmas.core.hostables.Artifact;
import ch.unisg.ics.interactions.hmas.core.hostables.HypermediaMASPlatform;
import ch.unisg.ics.interactions.hmas.core.hostables.Workspace;
import ch.unisg.ics.interactions.hmas.core.vocabularies.CORE;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphWriter;
import ch.unisg.ics.interactions.hmas.interaction.shapes.QualifiedValueSpecification;
import ch.unisg.ics.interactions.hmas.interaction.shapes.StringSpecification;
import ch.unisg.ics.interactions.hmas.interaction.shapes.ValueSpecification;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.*;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
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
public final class RepresentationFactoryHMASImpl implements RepresentationFactory {

  private final HttpInterfaceConfig httpConfig;

  public enum WebSubMode {
    subscribe,
    unsubscribe
  }

  public RepresentationFactoryHMASImpl(final HttpInterfaceConfig httpConfig) {
    this.httpConfig = httpConfig;
  }


  public Signifier webSubSignifier(String baseUri, String signifierName, String topic, WebSubMode mode) {
    return
      new Signifier.Builder(
        new ActionSpecification.Builder(
          new Form.Builder(this.httpConfig.getBaseUri() + "hub/")
            .setIRIAsString(baseUri + "#webSubForm")
            .setMethodName(HttpMethod.POST.name())
            .setContentType("application/json")
            .build())
          .setInputSpecification(
            new QualifiedValueSpecification.Builder()
              .setIRIAsString(baseUri + "#webSub" + mode.toString().substring(0,1).toUpperCase() + mode.toString().substring(1) + "Input")
              .addRequiredSemanticType("http://www.example.org/websub#websubsubscription")
              .setRequired(true)
              .addPropertySpecification("http://www.example.org/websub#topic",
                new StringSpecification.Builder()
                  .setRequired(true)
                  .setValue(topic)
                  .setName("hub.topic")
                  .setDescription("The topic of the WebSub hub")
                  .build()
              )
              .addPropertySpecification("http://www.example.org/websub#callback",
                new StringSpecification.Builder()
                  .setRequired(true)
                  .setName("hub.callback")
                  .setDescription("The callback URL of the WebSub hub")
                  .build()
              )
              .addPropertySpecification("http://www.example.org/websub#mode",
                new StringSpecification.Builder()
                  .setRequired(true)
                  .setValue(mode.name())
                  .setName("hub.mode")
                  .setDescription("The mode of the WebSub hub")
                  .build()
              ).build()
          ).build()
      )
        .setIRIAsString(baseUri + "#" + signifierName)
        .build();
  }


  @Override
  public String createPlatformRepresentation() {
    String baseUri = this.httpConfig.getBaseUri();
    String workspaces = this.httpConfig.getWorkspacesUri();
    HypermediaMASPlatform hypermediaMASPlatform = new HypermediaMASPlatform.Builder()
      .setIRIAsString(baseUri + "#platform")
      .addSemanticType("https://purl.org/hmas/HypermediaMASPlatform")
      .build();

    Form createWorkspaceFormJson = new Form.Builder(workspaces)
      .setIRIAsString(baseUri + "#createWorkspaceFormJson")
      .setMethodName(HttpMethod.POST.name())
      .build();

    Form createWorkspaceFormTxtTurtle = new Form.Builder(workspaces)
      .setIRIAsString(baseUri + "#createWorkspaceFormTextTurtle")
      .setMethodName(HttpMethod.POST.name())
      .setContentType("text/turtle")
      .build();

    Form sparqlGetQueryForm = new Form.Builder(baseUri + "query/")
      .setIRIAsString(baseUri + "#sparqlGetQueryForm")
      .setMethodName(HttpMethod.GET.name())
      .setContentType("application/sparql-query")
      .build();
    Form sparqlPostQueryForm = new Form.Builder(baseUri + "query/")
      .setIRIAsString(baseUri + "#sparqlPostQueryForm")
      .setMethodName(HttpMethod.POST.name())
      .setContentType("application/sparql-query")
      .build();

    ResourceProfile resourceProfile = new ResourceProfile.Builder(hypermediaMASPlatform)
      .setIRIAsString(baseUri)
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
          new ActionSpecification.Builder(sparqlGetQueryForm)
            .build()
        ).build())
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(sparqlPostQueryForm)
            .build()
        ).build())
      .exposeSignifier(webSubSignifier(baseUri,"subscribeToWorkspaces",baseUri + "workspaces/",WebSubMode.subscribe))
      .exposeSignifier(webSubSignifier(baseUri,"unsubscribeFromWorkspaces",baseUri + "workspaces/",WebSubMode.unsubscribe))
      .build();

    return serializeHmasResourceProfile(resourceProfile);
  }

  @Override
  public String createWorkspaceRepresentation(
      final String workspaceName,
      final Set<String> artifactTemplates
  ) {
    // TODO: Add artifactTemplates to makeArtifact signifier
    String baseUri = this.httpConfig.getWorkspaceUri(workspaceName);
    Workspace workspace = new Workspace.Builder()
      .setIRIAsString(baseUri + "#workspace")
      .addSemanticType("https://purl.org/hmas/Workspace")
      .build();

    // makeArtifact Signifier
    Form makeArtifactForm = new Form.Builder(baseUri + "artifacts/")
      .setMethodName(HttpMethod.POST.name())
      .setContentType("application/json")
      .setIRIAsString(baseUri + "#makeArtifactForm")
      .build();
    // TODO: Add inputSpecification to makeArtifact
    QualifiedValueSpecification makeArtifactInput = new QualifiedValueSpecification.Builder()
      .addRequiredSemanticType(CORE.TERM.ARTIFACT.toString())
      .setIRIAsString("http://example.org/artifact-shape")
      .setRequired(true)
      .addPropertySpecification("https://purl.org/hmas/jacamo/hasName",
        new StringSpecification.Builder()
          .setRequired(true)
          .setName("Name")
          .setDescription("The name of the created artifact")
          .build())
      .addPropertySpecification("https://purl.org/hmas/jacamo/hasClass",
        new StringSpecification.Builder()
          .setRequired(true)
          .setName("Class")
          .setDescription("The class of the created artifact")
          .build())
      .addPropertySpecification("https://purl.org/hmas/jacamo/hasInitialisationParameters",
        new ValueSpecification.Builder()
          .addRequiredSemanticType("http://www.w3.org/1999/02/22-rdf-syntax-ns#List")
          .setName("Initialization parameters")
          .setDescription("A list containing the parameters for initializing the artifact")
          .build())
      .build();

    // registerArtifact Signifier
    Form registerArtifactForm = new Form.Builder(baseUri + "artifacts/")
      .setMethodName(HttpMethod.POST.name())
      .setContentType("text/turtle")
      .setIRIAsString(baseUri + "#registerArtifactForm")
      .build();
    QualifiedValueSpecification registerArtifactInput = new QualifiedValueSpecification.Builder()
      .addRequiredSemanticType(CORE.TERM.ARTIFACT.toString())
      .setIRIAsString("http://example.org/artifact-shape")
      .setRequired(true)
      .addPropertySpecification("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString",
        new StringSpecification.Builder()
          .setRequired(true)
          .setName("representation")
          .setDescription("The representation of the artifact")
          .build())
      .build();

    // join Workspace Signifier
    Form joinWorkspaceForm = new Form.Builder(baseUri + "join/")
      .setMethodName(HttpMethod.POST.name())
      .setIRIAsString(baseUri + "#joinWorkspaceForm")
      .build();


    // leave Workspace Signifier
    Form leaveWorkspaceForm = new Form.Builder(baseUri + "leave/")
      .setMethodName(HttpMethod.POST.name())
      .setIRIAsString(baseUri + "#leaveWorkspaceForm")
      .build();

    // create SubWorkspace Signifier
    Form createSubWorkspaceForm = new Form.Builder(baseUri)
      .setMethodName(HttpMethod.POST.name())
      .setIRIAsString(baseUri + "#createSubWorkspaceForm")
      .build();

    // get current Workspace representation
    Form getCurrentWorkspaceForm = new Form.Builder(baseUri)
      .setMethodName(HttpMethod.GET.name())
      .setIRIAsString(baseUri + "#getCurrentWorkspaceForm")
      .build();

    // update current workspace representation
    Form updateCurrentWorkspaceForm = new Form.Builder(baseUri)
      .setMethodName(HttpMethod.PUT.name())
      .setIRIAsString(baseUri + "#updateCurrentWorkspaceForm")
      .build();

    // delete current workspace
    Form deleteCurrentWorkspaceForm = new Form.Builder(baseUri)
      .setMethodName(HttpMethod.DELETE.name())
      .setIRIAsString(baseUri + "#deleteCurrentWorkspaceForm")
      .build();



    var makeArtifactSignifier = new Signifier.Builder(new ActionSpecification.Builder(makeArtifactForm)
      .setInputSpecification(makeArtifactInput).build())
      .build();
    var registerArtifactSignifier = new Signifier.Builder(new ActionSpecification.Builder(registerArtifactForm)
      .setInputSpecification(registerArtifactInput).build())
      .build();
    var joinWorkspaceSignifier = new Signifier.Builder(new ActionSpecification.Builder(joinWorkspaceForm).build()).build();
    var leaveWorkspaceSignifier = new Signifier.Builder(new ActionSpecification.Builder(leaveWorkspaceForm).build()).build();
    var createSubWorkspaceSignifier = new Signifier.Builder(new ActionSpecification.Builder(createSubWorkspaceForm).build()).build();
    var getCurrentWorkspaceSignifier = new Signifier.Builder(new ActionSpecification.Builder(getCurrentWorkspaceForm).build()).build();
    var updateCurrentWorkspaceSignifier = new Signifier.Builder(new ActionSpecification.Builder(updateCurrentWorkspaceForm).build()).build();
    var deleteCurrentWorkspaceSignifier = new Signifier.Builder(new ActionSpecification.Builder(deleteCurrentWorkspaceForm).build()).build();




    ResourceProfile resourceProfile = new ResourceProfile.Builder(workspace)
      .setIRIAsString(this.httpConfig.getWorkspaceUri(workspaceName))
      .exposeSignifier(makeArtifactSignifier)
      .exposeSignifier(registerArtifactSignifier)
      .exposeSignifier(joinWorkspaceSignifier)
      .exposeSignifier(leaveWorkspaceSignifier)
      .exposeSignifier(createSubWorkspaceSignifier)
      .exposeSignifier(getCurrentWorkspaceSignifier)
      .exposeSignifier(updateCurrentWorkspaceSignifier)
      .exposeSignifier(deleteCurrentWorkspaceSignifier)
      .exposeSignifier(webSubSignifier(baseUri,"subscribeToWorkspace",baseUri,WebSubMode.subscribe))
      .exposeSignifier(webSubSignifier(baseUri,"unsubscribeFromWorkspace",baseUri,WebSubMode.unsubscribe))
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

    // focus this artifact
    Form focusArtifactForm = new Form.Builder(baseUri + "focus/")
      .setMethodName(HttpMethod.POST.name())
      .setIRIAsString(baseUri + "#focusArtifactForm")
      .build();

    resourceProfileBuilder
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(getArtifactRepresentationForm)
            .build()
        ).setIRIAsString(baseUri + "#getArtifactRepresentation")
          .build())
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(updateArtifactForm)
            .build()
        ).setIRIAsString(baseUri + "#updateArtifact")
          .build())
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(deleteArtifactForm)
            .build()
        ).setIRIAsString(baseUri + "#deleteArtifact")
          .build())
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(focusArtifactForm)
            .build()
        ).setIRIAsString(baseUri + "#focusArtifact")
          .build()
      )
      .exposeSignifier(webSubSignifier(baseUri,"subscribeToArtifact",baseUri,WebSubMode.subscribe))
      .exposeSignifier(webSubSignifier(baseUri,"unsubscribeFromArtifact",baseUri,WebSubMode.unsubscribe));

    return serializeHmasResourceProfile(resourceProfileBuilder.build());
  }

  @Override
  public String createBodyRepresentation(
      final String workspaceName,
      final String agentName,
      final Model metadata
  ) {
    String baseUri = this.httpConfig.getAgentBodyUri(workspaceName, agentName);

    // TODO: set #body everywhere
    Artifact agent = new Artifact.Builder()
      .setIRIAsString(baseUri + "#artifact")
      .addSemanticType("https://purl.org/hmas/jacamo/Body")
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
        ).build())
      .exposeSignifier(webSubSignifier(baseUri,"subscribeToAgent",baseUri,WebSubMode.subscribe))
      .exposeSignifier(webSubSignifier(baseUri,"subscribeToAgent",baseUri,WebSubMode.unsubscribe));


    return serializeHmasResourceProfile(profile.build());
  }

  @Override
  public String createArtifactRepresentation(String workspaceName, String artifactName, SecurityScheme securityScheme, String semanticType, Model metadata, ListMultimap<String, ActionAffordance> actionAffordances) {
    return null;
  }

  @Override
  public String createBodyRepresentation(String workspaceName, String agentName, SecurityScheme securityScheme, Model metadata) {
    return null;
  }

  private String serializeHmasResourceProfile(final ResourceProfile profile) {
    return new ResourceProfileGraphWriter(profile)
      .setNamespace("hmas","https://purl.org/hmas/")
      .setNamespace("jacamo","https://purl.org/hmas/jacamo/")
      .setNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
      .setNamespace("websub", "http://www.example.org/websub#")
      .write();
  }
}
