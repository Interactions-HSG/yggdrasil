package org.hyperagents.yggdrasil.utils.impl;


import ch.unisg.ics.interactions.hmas.core.hostables.Artifact;
import ch.unisg.ics.interactions.hmas.core.hostables.HypermediaMASPlatform;
import ch.unisg.ics.interactions.hmas.core.hostables.Workspace;
import ch.unisg.ics.interactions.hmas.core.vocabularies.CORE;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphWriter;
import ch.unisg.ics.interactions.hmas.interaction.shapes.QualifiedValueSpecification;
import ch.unisg.ics.interactions.hmas.interaction.shapes.StringSpecification;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.*;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import io.vertx.core.http.HttpMethod;


import java.util.HashMap;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;


import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.RepresentationFactory;
import org.hyperagents.yggdrasil.utils.WebSubConfig;

/**
 * This class is an implementation of the RepresentationFactory interface. It provides methods to create representations
 * of platforms, workspaces, artifacts, and bodies. The representations are serialized as Thing Descriptions using the
 * TDGraphWriter class. The class also includes helper methods for serializing Thing Descriptions.
 */
public final class RepresentationFactoryHMASImpl implements RepresentationFactory {



  private final static String CONTENT_TYPE_TURTLE = "text/turtle";
  private final static String HMAS = "https://purl.org/hmas/";
  private final static String JACAMO = "https://purl.org/hmas/jacamo/";

  private final HttpInterfaceConfig httpConfig;

  private final WebSubConfig notificationConfig;

  private final String baseUri;

  public enum WebSubMode {
    subscribe,
    unsubscribe
  }

  private record WebSubSpecs(String baseUri,String signifierName,String actionType, String topic){}

  public RepresentationFactoryHMASImpl(final HttpInterfaceConfig httpConfig, final WebSubConfig notificationConfig) {
    this.httpConfig = httpConfig;
    this.notificationConfig = notificationConfig;
    this.baseUri = httpConfig.getBaseUri();
  }


  private Signifier webSubSignifier(final WebSubSpecs specs,
                                   final WebSubMode mode) {
    return
      new Signifier.Builder(
        new ActionSpecification.Builder(
          new Form.Builder(notificationConfig.getWebSubHubUri())
            .setIRIAsString(specs.baseUri() + "#webSubForm")
            .setMethodName(HttpMethod.POST.name())
            .setContentType("application/json")
            .build())
          .addRequiredSemanticType(specs.actionType())
          .setInputSpecification(
            new QualifiedValueSpecification.Builder()
              .setIRIAsString(specs.baseUri() + "#webSub" + mode.toString().substring(0, 1).toUpperCase(Locale.ENGLISH) + mode.toString().substring(1) + "Input")
              .addRequiredSemanticType("http://www.example.org/websub#websubsubscription")
              .setRequired(true)
              .addPropertySpecification("http://www.example.org/websub#topic",
                new StringSpecification.Builder()
                  .setRequired(true)
                  .setValue(specs.topic())
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
        .setIRIAsString(baseUri + "#" + specs.signifierName())
        .build();
  }


  public void addWebSubSignifier(final ResourceProfile.Builder profile,
                                 final String signifierName,
                                 final String actionType,
                                 final String topic) {
    if (this.notificationConfig.isEnabled()) {
      profile.exposeSignifier(webSubSignifier(new WebSubSpecs(this.baseUri, "subscribeTo" + signifierName, JACAMO + "Observe" + actionType,topic),WebSubMode.subscribe));
      profile.exposeSignifier(webSubSignifier(new WebSubSpecs(this.baseUri, "unsubscribeFrom" + signifierName, JACAMO + "Unobserve" + actionType,topic),WebSubMode.unsubscribe));
    }
  }

  public void addWebSubSignifier(final ResourceProfile.Builder profile,final String signifierName, final String actionType) {
    addWebSubSignifier(profile, signifierName,actionType, this.httpConfig.getBaseUri());
  }

  @Override
  public String createPlatformRepresentation() {
    final String baseUri = this.httpConfig.getBaseUri();
    final String workspaces = this.httpConfig.getWorkspacesUri();
    final HypermediaMASPlatform hypermediaMASPlatform = new HypermediaMASPlatform.Builder()
      .setIRIAsString(baseUri + "#platform")
      .addSemanticType(HMAS + "HypermediaMASPlatform")
      .build();

    final Form createWorkspaceFormJson = new Form.Builder(workspaces)
      .setIRIAsString(baseUri + "#createWorkspaceFormJson")
      .setMethodName(HttpMethod.POST.name())
      .build();

    final Form createWorkspaceFormTxtTurtle = new Form.Builder(workspaces)
      .setIRIAsString(baseUri + "#createWorkspaceFormTextTurtle")
      .setMethodName(HttpMethod.POST.name())
      .setContentType(CONTENT_TYPE_TURTLE)
      .build();

    final Form sparqlGetQueryForm = new Form.Builder(baseUri + "query/")
      .setIRIAsString(baseUri + "#sparqlGetQueryForm")
      .setMethodName(HttpMethod.GET.name())
      .setContentType("application/sparql-query")
      .build();
    final Form sparqlPostQueryForm = new Form.Builder(baseUri + "query/")
      .setIRIAsString(baseUri + "#sparqlPostQueryForm")
      .setMethodName(HttpMethod.POST.name())
      .setContentType("application/sparql-query")
      .build();

    final var resourceProfile = new ResourceProfile.Builder(hypermediaMASPlatform)
      .setIRIAsString(baseUri)
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(createWorkspaceFormJson)
            .addRequiredSemanticType(JACAMO + "MakeWorkspace")
            .build()
        ).setIRIAsString(baseUri + "#createWorkspaceJson")
          .build())
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(createWorkspaceFormTxtTurtle)
            .addRequiredSemanticType(JACAMO + "MakeWorkspace")
            .build()
        ).setIRIAsString(baseUri + "#createWorkspaceTurtle").build())
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(sparqlGetQueryForm)
            .build()
        ).setIRIAsString(baseUri + "#sparglGetQuery").build())
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(sparqlPostQueryForm)
            .build()
        ).setIRIAsString(baseUri + "#sparqlPostQuery").build());

    addWebSubSignifier(resourceProfile, "Workspaces", "Platform", baseUri + "workspaces/");

    return serializeHmasResourceProfile(resourceProfile.build());
  }

  @Override
  public String createWorkspaceRepresentation(
    final String workspaceName,
    final Set<String> artifactTemplates
  ) {
    // TODO: Add artifactTemplates to makeArtifact signifier
    final String baseUri = this.httpConfig.getWorkspaceUri(workspaceName);
    final Workspace workspace = new Workspace.Builder()
      .setIRIAsString(baseUri + "#workspace")
      .addSemanticType(HMAS + "Workspace")
      .build();

    // makeArtifact Signifier
    final Form makeArtifactForm = new Form.Builder(baseUri + "artifacts/")
      .setMethodName(HttpMethod.POST.name())
      .setContentType("application/json")
      .setIRIAsString(baseUri + "#makeArtifactForm")
      .build();
    // TODO: Add inputSpecification to makeArtifact
    final QualifiedValueSpecification makeArtifactInput = new QualifiedValueSpecification.Builder()
      .addRequiredSemanticType(CORE.TERM.ARTIFACT.toString())
      .setIRIAsString("http://example.org/artifact-shape")
      .setRequired(true)
      .addPropertySpecification(JACAMO + "hasName",
        new StringSpecification.Builder()
          .setRequired(true)
          .setName("Name")
          .setDescription("The name of the created artifact")
          .build())
      .addPropertySpecification(JACAMO + "hasClass",
        new StringSpecification.Builder()
          .setRequired(true)
          .setName("Class")
          .setDescription("The class of the created artifact")
          .build())
      .addPropertySpecification(JACAMO + "hasInitialisationParameters",
        new StringSpecification.Builder()
          .setName("Initialization parameters")
          .setDescription("A list containing the parameters for initializing the artifacts")
          .build())
      .build();

    // registerArtifact Signifier
    final Form registerArtifactForm = new Form.Builder(baseUri + "artifacts/")
      .setMethodName(HttpMethod.POST.name())
      .setContentType(CONTENT_TYPE_TURTLE)
      .setIRIAsString(baseUri + "#registerArtifactForm")
      .build();
    final QualifiedValueSpecification registerArtifactInput = new QualifiedValueSpecification.Builder()
      .addRequiredSemanticType(CORE.TERM.ARTIFACT.toString())
      .setIRIAsString("http://example.org/artifact-rdf")
      .setRequired(true)
      .addPropertySpecification("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString",
        new StringSpecification.Builder()
          .setRequired(true)
          .setName("representation")
          .setDescription("The representation of the artifact")
          .build())
      .build();

    // join Workspace Signifier
    final Form joinWorkspaceForm = new Form.Builder(baseUri + "join/")
      .setMethodName(HttpMethod.POST.name())
      .setIRIAsString(baseUri + "#joinWorkspaceForm")
      .build();


    // leave Workspace Signifier
    final Form leaveWorkspaceForm = new Form.Builder(baseUri + "leave/")
      .setMethodName(HttpMethod.POST.name())
      .setIRIAsString(baseUri + "#leaveWorkspaceForm")
      .build();

    // create SubWorkspace Signifier
    final Form createSubWorkspaceForm = new Form.Builder(baseUri)
      .setMethodName(HttpMethod.POST.name())
      .setIRIAsString(baseUri + "#createSubWorkspaceForm")
      .build();

    // get current Workspace representation
    final Form getCurrentWorkspaceForm = new Form.Builder(baseUri)
      .setMethodName(HttpMethod.GET.name())
      .setIRIAsString(baseUri + "#getCurrentWorkspaceForm")
      .setContentType(CONTENT_TYPE_TURTLE)
      .build();

    // update current workspace representation
    final Form updateCurrentWorkspaceForm = new Form.Builder(baseUri)
      .setMethodName(HttpMethod.PUT.name())
      .setIRIAsString(baseUri + "#updateCurrentWorkspaceForm")
      .setContentType(CONTENT_TYPE_TURTLE)
      .build();

    // delete current workspace
    final Form deleteCurrentWorkspaceForm = new Form.Builder(baseUri)
      .setMethodName(HttpMethod.DELETE.name())
      .setIRIAsString(baseUri + "#deleteCurrentWorkspaceForm")
      .build();


    final var makeArtifactSignifier =
      new Signifier.Builder(new ActionSpecification.Builder(makeArtifactForm)
        .addRequiredSemanticType(JACAMO + "MakeArtifact")
        .setInputSpecification(makeArtifactInput).build())
        .setIRIAsString(baseUri + "#makeArtifact")
        .build();
    final var registerArtifactSignifier =
      new Signifier.Builder(new ActionSpecification.Builder(registerArtifactForm)
        .addRequiredSemanticType(JACAMO + "RegisterArtifact")
        .setInputSpecification(registerArtifactInput).build())
        .setIRIAsString(baseUri + "#registerArtifact")
        .build();
    final var joinWorkspaceSignifier =
      new Signifier.Builder(new ActionSpecification.Builder(joinWorkspaceForm)
        .addRequiredSemanticType(JACAMO + "JoinWorkspace")
      .build())
        .setIRIAsString(baseUri + "#joinWorkspace")
        .build();
    final var leaveWorkspaceSignifier =
      new Signifier.Builder(new ActionSpecification.Builder(leaveWorkspaceForm)
        .addRequiredSemanticType(JACAMO + "LeaveWorkspace")
      .build())
        .setIRIAsString(baseUri + "#leaveWorkspace")
        .build();
    final var createSubWorkspaceSignifier =
      new Signifier.Builder(new ActionSpecification.Builder(createSubWorkspaceForm)
        .addRequiredSemanticType(JACAMO + "MakeWorkspace")
      .build())
        .setIRIAsString(baseUri + "#createSubWorkspace")
        .build();
    final var getCurrentWorkspaceSignifier =
      new Signifier.Builder(new ActionSpecification.Builder(getCurrentWorkspaceForm)
        .addRequiredSemanticType(JACAMO + "PerceiveWorkspace")
      .build())
        .setIRIAsString(baseUri + "#getCurrentWorkspace")
        .build();
    final var updateCurrentWorkspaceSignifier =
      new Signifier.Builder(new ActionSpecification.Builder(updateCurrentWorkspaceForm)
        .addRequiredSemanticType(JACAMO + "UpdateWorkspace")
      .build())
        .setIRIAsString(baseUri + "#updateCurrentWorkspace")
        .build();
    final var deleteCurrentWorkspaceSignifier =
      new Signifier.Builder(new ActionSpecification.Builder(deleteCurrentWorkspaceForm)
        .addRequiredSemanticType(JACAMO + "DeleteWorkspace")
      .build())
        .setIRIAsString(baseUri + "#deleteCurrentWorkspace")
        .build();

    final var resourceProfile = new ResourceProfile.Builder(workspace)
      .setIRIAsString(baseUri.substring(0, baseUri.length() - 1))
      .exposeSignifier(makeArtifactSignifier)
      .exposeSignifier(registerArtifactSignifier)
      .exposeSignifier(joinWorkspaceSignifier)
      .exposeSignifier(leaveWorkspaceSignifier)
      .exposeSignifier(createSubWorkspaceSignifier)
      .exposeSignifier(getCurrentWorkspaceSignifier)
      .exposeSignifier(updateCurrentWorkspaceSignifier)
      .exposeSignifier(deleteCurrentWorkspaceSignifier);

    addWebSubSignifier(resourceProfile, "Workspace", "Workspace");

    return serializeHmasResourceProfile(resourceProfile.build());

  }

  @Override
  public String createArtifactRepresentation(final String workspaceName, final String artifactName,
                                             final String semanticType) {
    return createArtifactRepresentation(
      workspaceName,
      artifactName,
      semanticType,
      new LinkedHashModel(),
      Multimaps.newListMultimap(new HashMap<>(), ArrayList::new)
    );
  }

  @Override
  public String createArtifactRepresentation(
    final String workspaceName,
    final String artifactName,
    final String semanticType,
    final Model metadata,
    final ListMultimap<String, Object> signifiers
  ) {
    final String baseUri = this.httpConfig.getArtifactUri(workspaceName, artifactName);

    final Artifact artifact = new Artifact.Builder()
      .addSemanticType(semanticType)
      .setIRIAsString(baseUri + "#artifact") //  #artifact
      .build();

    final ResourceProfile.Builder resourceProfileBuilder = new ResourceProfile.Builder(artifact)
      .setIRIAsString(baseUri.substring(0, baseUri.length() - 1));
    signifiers.values().forEach(obj -> resourceProfileBuilder.exposeSignifier((Signifier) obj));

    // add Signifiers that are always given
    // get the representation for this artifact
    final Form getArtifactRepresentationForm = new Form.Builder(baseUri)
      .setMethodName(HttpMethod.GET.name())
      .setIRIAsString(baseUri + "#getArtifactRepresentationForm")
      .setContentType(CONTENT_TYPE_TURTLE)
      .build();

    // update this artifact
    final Form updateArtifactForm = new Form.Builder(baseUri)
      .setMethodName(HttpMethod.PUT.name())
      .setIRIAsString(baseUri + "#updateArtifactForm")
      .setContentType(CONTENT_TYPE_TURTLE)
      .build();

    // delete this artifact
    final Form deleteArtifactForm = new Form.Builder(baseUri)
      .setMethodName(HttpMethod.DELETE.name())
      .setIRIAsString(baseUri + "#deleteArtifactForm")
      .build();

    // focus this artifact
    final Form focusArtifactForm = new Form.Builder(baseUri + "focus/")
      .setMethodName(HttpMethod.POST.name())
      .setIRIAsString(baseUri + "#focusArtifactForm")
      .build();

    resourceProfileBuilder
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(getArtifactRepresentationForm)
            .addRequiredSemanticType(JACAMO + "PerceiveArtifact")
            .build()
        ).setIRIAsString(baseUri + "#getArtifactRepresentation")
          .build())
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(updateArtifactForm)
            .addRequiredSemanticType(JACAMO + "UpdateArtifact")
            .build()
        ).setIRIAsString(baseUri + "#updateArtifact")
          .build())
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(deleteArtifactForm)
            .addRequiredSemanticType(JACAMO + "DeleteArtifact")
            .build()
        ).setIRIAsString(baseUri + "#deleteArtifact")
          .build())
      .exposeSignifier(
        new Signifier.Builder(
          new ActionSpecification.Builder(focusArtifactForm)
            .addRequiredSemanticType(JACAMO + "Focus")
            .build()
        ).setIRIAsString(baseUri + "#focusArtifact")
          .build()
      );

    addWebSubSignifier(resourceProfileBuilder, "Artifact", "Artifact");

    return serializeHmasResourceProfile(resourceProfileBuilder.build());
  }

  @Override
  public String createBodyRepresentation(
    final String workspaceName,
    final String agentName,
    final Model metadata
  ) {
    final String baseUri = this.httpConfig.getAgentBodyUri(workspaceName, agentName);

    final Artifact agent = new Artifact.Builder()
      .setIRIAsString(baseUri + "#artifact")
      .addSemanticType(JACAMO + "Body")
      .build();

    final ResourceProfile.Builder profile = new ResourceProfile.Builder(agent)
      .setIRIAsString(baseUri.substring(0, baseUri.length() - 1));

    // Possible Signifiers of body
    final Form getBodyRepresentationForm = new Form.Builder(baseUri)
      .setMethodName(HttpMethod.GET.name())
      .setIRIAsString(baseUri + "#getBodyRepresentationForm")
      .setContentType(CONTENT_TYPE_TURTLE)
      .build();

    final Form updateBodyForm = new Form.Builder(baseUri)
      .setMethodName(HttpMethod.PUT.name())
      .setIRIAsString(baseUri + "#updateBodyForm")
      .setContentType(CONTENT_TYPE_TURTLE)
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

    addWebSubSignifier(profile, "Agent", "Artifact");

    return serializeHmasResourceProfile(profile.build());
  }

  @Override
  public String createArtifactRepresentation(final String workspaceName, final String artifactName,
                                             final SecurityScheme securityScheme, final String semanticType,
                                             final Model metadata,
                                             final ListMultimap<String, Object> actionAffordances) {
    return createArtifactRepresentation(
      workspaceName,
      artifactName,
      semanticType,
      metadata,
      actionAffordances
    );
  }

  @Override
  public String createBodyRepresentation(final String workspaceName, final String agentName,
                                         final SecurityScheme securityScheme, final Model metadata) {
    return createBodyRepresentation(
      workspaceName,
      agentName,
      metadata
    );
  }

  private String serializeHmasResourceProfile(final ResourceProfile profile) {
    return new ResourceProfileGraphWriter(profile)
      .setNamespace("hmas", HMAS)
      .setNamespace("jacamo", JACAMO)
      .setNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
      .setNamespace("websub", "http://www.example.org/websub#")
      .write();
  }
}
