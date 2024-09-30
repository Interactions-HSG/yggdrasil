package org.hyperagents.yggdrasil.utils.impl;

import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import io.vertx.core.http.HttpMethod;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.RdfModelUtils;
import org.hyperagents.yggdrasil.utils.RepresentationFactory;
import org.hyperagents.yggdrasil.utils.WebSubConfig;

/**
 * This class is an implementation of the RepresentationFactory interface. It provides methods to
 * create representations of platforms, workspaces, artifacts, and bodies. The representations are
 * serialized as Thing Descriptions using the TDGraphWriter class. The class also includes helper
 * methods for serializing Thing Descriptions.
 */
public class RepresentationFactoryTDImplt implements RepresentationFactory {
  private static final String ARTIFACT_NAME_PARAM = "artifactName";
  private static final String ARTIFACT = "Artifact";

  private final HttpInterfaceConfig httpConfig;
  private final WebSubConfig notificationConfig;

  private static final String HMAS = "https://purl.org/hmas/";
  private static final String JACAMO = HMAS + "jacamo/";
  private static final String HASH_ARTIFACT = "#artifact";

  private static final String GET = HttpMethod.GET.name();
  private static final String POST = HttpMethod.POST.name();
  private static final String DELETE = HttpMethod.DELETE.name();
  private static final String PUT = HttpMethod.PUT.name();

  public RepresentationFactoryTDImplt(final HttpInterfaceConfig httpConfig,
                                      final WebSubConfig notificationConfig) {
    this.httpConfig = httpConfig;
    this.notificationConfig = notificationConfig;
  }

  private void addHttpSignifiers(final ThingDescription.Builder td, final String target,
                                 final String type) {
    addAction(td, "get" + type + "Representation", target, GET, "Perceive" + type);
    addAction(td, "update" + type + "Representation", target, PUT, "Update" + type);
    addAction(td, "delete" + type + "Representation", target, DELETE, "Delete" + type);
  }

  private void addAction(final ThingDescription.Builder thingDescription,
                         final String name,
                         final String target,
                         final String methodName,
                         final String semanticType) {
    addAction(thingDescription, name, target, "application/json", methodName,
        semanticType);
  }

  private void addAction(final ThingDescription.Builder thingDescription,
                         final String name,
                         final String target,
                         final String contentType,
                         final String methodName,
                         final String semanticType) {
    thingDescription.addAction(
        new ActionAffordance.Builder(
            name,
            new Form.Builder(target)
                .setMethodName(methodName)
                .setContentType(contentType)
                .build()
        ).addSemanticType(JACAMO + semanticType).build()
    );
  }

  private void addWebSub(final ThingDescription.Builder td, final String actionName) {
    if (notificationConfig.isEnabled()) {
      td.addAction(websubActions("subscribeTo" + actionName));
      td.addAction(websubActions("unsubscribeFrom" + actionName));
    }
  }

  private ActionAffordance websubActions(final String actionName) {
    return new ActionAffordance.Builder(
        actionName,
        new Form.Builder(this.notificationConfig.getWebSubHubUri())
            .setMethodName(HttpMethod.POST.name())
            .setContentType("application/json")
            .addSubProtocol("websub")// could be used for websub
            .build()
    ).addInputSchema(
            new ObjectSchema
                .Builder()
                .addProperty("callbackIri", new StringSchema.Builder().build())
                .addProperty("mode", (new StringSchema.Builder()).build())
                .addProperty("topic", new StringSchema.Builder().build())
                .build()
        ).addSemanticType("https://purl.org/hmas/websub/" + actionName)
        .build();
  }

  private void wrapInResourceProfile(final ThingDescription.Builder td, final String thingIRI,
                                     final String tdIRI) {
    final Model graph = new LinkedHashModel();

    graph.add(
        RdfModelUtils.createIri(thingIRI),
        RDF.TYPE,
        RdfModelUtils.createIri(HMAS + "ResourceProfile")
    );

    graph.add(
        RdfModelUtils.createIri(thingIRI),
        RdfModelUtils.createIri(HMAS + "isProfileOf"),
        RdfModelUtils.createIri(tdIRI)
    );
    td.addGraph(graph);

  }

  @Override
  public String createPlatformRepresentation() {
    final var thingIri = this.httpConfig.getBaseUriTrailingSlash()
        .substring(0, this.httpConfig.getBaseUriTrailingSlash().length() - 1);
    final var td = new ThingDescription.Builder("Yggdrasil Node")
        .addThingURI(thingIri + "/#platform")
        .addSemanticType(HMAS + "HypermediaMASPlatform");

    addAction(td, "createWorkspaceJson", this.httpConfig.getWorkspacesUri(), POST,
        "makeWorkspace");
    addAction(td, "createWorkspaceTurtle", this.httpConfig.getWorkspacesUri(),
        "text/turtle", POST, "createWorkspace");

    addAction(td, "sparqlGetQuery", this.httpConfig.getBaseUriTrailingSlash() + "query/",
        "application/sparql-query", GET, "sparqlGetQuery");
    addAction(td, "sparqlPostQuery", this.httpConfig.getBaseUriTrailingSlash() + "query/",
        "application/sparql-query", POST, "sparqlPostQuery");

    addWebSub(td, "Workspaces");

    wrapInResourceProfile(td, thingIri + "/", thingIri + "/#platform");

    return serializeThingDescription(
        td
    );
  }

  @Override
  public String createWorkspaceRepresentation(
      final String workspaceName,
      final Set<String> artifactTemplates,
      final boolean isCartagoWorkspace
  ) {
    final var thingUri = this.httpConfig.getWorkspaceUriTrailingSlash(workspaceName).substring(0,
        this.httpConfig.getWorkspaceUriTrailingSlash(workspaceName).length() - 1);
    final var td =
        new ThingDescription
            .Builder(workspaceName)
            .addThingURI(thingUri + "/#workspace")
            .addSemanticType(HMAS + "Workspace");

    addAction(td, "createSubWorkspaceJson", thingUri, POST, "makeSubWorkspace");
    addAction(td, "createSubWorkspaceTurtle",  thingUri,
        "text/turtle", POST, "createSubWorkspace");

    addHttpSignifiers(td, thingUri, "Workspace");

    addAction(td, "createArtifact", this.httpConfig.getArtifactsUri(workspaceName),
        "text/turtle", POST, "createArtifact");


    if (isCartagoWorkspace) {
      addAction(td, "joinWorkspace", thingUri + "/join", POST, "JoinWorkspace");
      addAction(td, "quitWorkspace", thingUri + "/leave", POST, "QuitWorkspace");
      td.addAction(
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
                                  .addSemanticType(JACAMO + "ArtifactTemplate")
                                  .build()
                          )
                          .addProperty(ARTIFACT_NAME_PARAM,
                              new StringSchema.Builder().addSemanticType(JACAMO + "ArtifactName")
                                  .build())
                          .addProperty("initParams",
                              new ArraySchema.Builder()
                                  .addSemanticType(JACAMO + "InitParams").build())
                          .addRequiredProperties("artifactClass", ARTIFACT_NAME_PARAM)
                          .build()
                  ).addSemanticType(JACAMO + "MakeArtifact")
                  .build()
          )
          .addAction(
              new ActionAffordance.Builder(
                  "focus",
                  new Form.Builder(thingUri + "/focus")
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
                  ).addSemanticType(JACAMO + "Focus")
                  .build()
          );
    }

    addWebSub(td, "Workspace");
    wrapInResourceProfile(td, thingUri, thingUri + "/#workspace");
    return serializeThingDescription(td);
  }

  @Override
  public String createArtifactRepresentation(final String workspaceName, final String artifactName,
                                             final String semanticType,
                                             final boolean isCartagoArtifact) {
    return createArtifactRepresentation(
        workspaceName,
        artifactName,
        SecurityScheme.getNoSecurityScheme(),
        semanticType,
        new LinkedHashModel(),
        Multimaps.newListMultimap(new HashMap<>(), ArrayList::new),
        isCartagoArtifact
    );
  }

  @Override
  public String createArtifactRepresentation(final String workspaceName, final String artifactName,
                                             final String semanticType, final Model metadata,
                                             final ListMultimap<String, Object> actionAffordances,
                                             final boolean isCartagoArtifact) {
    return createArtifactRepresentation(
        workspaceName,
        artifactName,
        SecurityScheme.getNoSecurityScheme(),
        semanticType,
        metadata,
        actionAffordances,
        isCartagoArtifact
    );
  }


  @Override
  public String createArtifactRepresentation(
      final String workspaceName,
      final String artifactName,
      final SecurityScheme securityScheme,
      final String semanticType,
      final Model metadata,
      final ListMultimap<String, Object> actionAffordances,
      final boolean isCartagoArtifact
  ) {
    final ListMultimap<String, ActionAffordance> actionAffordancesMap =
        Multimaps.newListMultimap(new HashMap<>(),
            ArrayList::new);
    actionAffordances.entries().forEach(entry -> {
      final var actionName = entry.getKey();
      final var action = (ActionAffordance) entry.getValue();
      actionAffordancesMap.put(actionName, action);
    });


    final var thingUri = this.httpConfig.getArtifactUri(workspaceName, artifactName);

    final var td =
        new ThingDescription.Builder(artifactName)
            .addSecurityScheme(securityScheme.getSchemeName(), securityScheme)
            .addSemanticType(HMAS + ARTIFACT)
            .addSemanticType(semanticType)
            .addThingURI(thingUri + "/" + HASH_ARTIFACT)
            .addGraph(metadata);

    actionAffordancesMap.values().forEach(td::addAction);

    addHttpSignifiers(td, thingUri, ARTIFACT);

    if (isCartagoArtifact) {
      addAction(td, "focusArtifact", thingUri + "focus/", HttpMethod.POST.name(), "Focus");
    }

    addWebSub(td, ARTIFACT);
    wrapInResourceProfile(td, thingUri, thingUri + "/" + HASH_ARTIFACT);

    return serializeThingDescription(td);
  }

  @Override
  public String createBodyRepresentation(
      final String workspaceName,
      final String agentName,
      final Model metadata) {
    return createBodyRepresentation(workspaceName, agentName, SecurityScheme.getNoSecurityScheme(),
        metadata);
  }

  @Override
  public String createBodyRepresentation(
      final String workspaceName,
      final String agentName,
      final SecurityScheme securityScheme,
      final Model metadata
  ) {
    final var bodyUri = this.httpConfig.getAgentBodyUri(workspaceName, agentName);
    final var td =
        new ThingDescription
            .Builder(agentName)
            .addSecurityScheme(securityScheme.getSchemeName(), securityScheme)
            .addSemanticType(HMAS + ARTIFACT)
            .addSemanticType(JACAMO + "Body")
            .addThingURI(bodyUri + "/" + HASH_ARTIFACT)
            .addGraph(metadata);
    addWebSub(td, "Agent");
    wrapInResourceProfile(td, bodyUri, bodyUri + "/" + HASH_ARTIFACT);
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
        .setNamespace("hmas", HMAS)
        .setNamespace("ex", "http://example.org/")
        .setNamespace("jacamo", JACAMO)
        .setNamespace("websub", "http://example.org/websub#")
        .write();
  }
}
