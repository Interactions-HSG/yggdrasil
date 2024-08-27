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
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;
import org.hyperagents.yggdrasil.utils.RdfModelUtils;
import org.hyperagents.yggdrasil.utils.RepresentationFactory;
import org.hyperagents.yggdrasil.utils.WebSubConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class RepresentationFactoryTDImplt implements RepresentationFactory {
  private static final String ARTIFACT_NAME_PARAM = "artifactName";

  private final HttpInterfaceConfig httpConfig;
  private final WebSubConfig notificationConfig;

  private static final String HMAS = "https://purl.org/hmas/";
  private static final String JACAMO = HMAS + "jacamo/";
  private static final String HASH_ARTIFACT = "#artifact";

  public RepresentationFactoryTDImplt(final HttpInterfaceConfig httpConfig, final WebSubConfig notificationConfig) {
    this.httpConfig = httpConfig;
    this.notificationConfig = notificationConfig;
  }

  private void addWebSub(final ThingDescription.Builder td,final String actionName) {
  if (notificationConfig.isEnabled()) {
    td.addAction(websubActions("subscribeTo" + actionName));
    td.addAction(websubActions("unsubscribeFrom" + actionName));
  }}

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

  private void wrapInResourceProfile(final ThingDescription.Builder td,final String thingIRI,final String tdIRI) {
    final Model graph = new LinkedHashModel();

    graph.add(
      RdfModelUtils.createIri(thingIRI.substring(0, thingIRI.length() - 1)),
      RDF.TYPE,
      RdfModelUtils.createIri(HMAS + "ResourceProfile")
    );

    graph.add(
      RdfModelUtils.createIri(thingIRI.substring(0, thingIRI.length() - 1)),
      RdfModelUtils.createIri(HMAS + "isProfileOf"),
      RdfModelUtils.createIri(tdIRI)
    );
    td.addGraph(graph);

  }

  @Override
  public String createPlatformRepresentation() {
    final var thingIri = this.httpConfig.getBaseUri();
    final var td = new ThingDescription.Builder("Yggdrasil Node")
      .addThingURI(thingIri + "#platform")
      .addSemanticType(HMAS + "HypermediaMASPlatform")
      .addAction(new ActionAffordance.Builder(
        "createWorkspace",
        new Form.Builder(this.httpConfig.getWorkspacesUri())
          .setMethodName(HttpMethod.POST.name())
          .build())
        .addSemanticType(JACAMO + "createWorkspace")
        .build()
      );

    addWebSub(td, "Workspaces");

    wrapInResourceProfile(td, thingIri + "/", thingIri + "#platform");

    return serializeThingDescription(
      td
    );
  }

  @Override
  public String createWorkspaceRepresentation(
    final String workspaceName,
    final Set<String> artifactTemplates
  ) {
    final var thingUri = this.httpConfig.getWorkspaceUri(workspaceName);
    final var td =
      new ThingDescription
        .Builder(workspaceName)
        .addThingURI(thingUri + "#workspace")
        .addSemanticType(HMAS + "Workspace")
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
                    .addSemanticType(JACAMO + "ArtifactTemplate")
                    .build()
                )
                .addProperty(ARTIFACT_NAME_PARAM,
                  new StringSchema.Builder().addSemanticType(JACAMO + "ArtifactName").build())
                .addProperty("initParams", new ArraySchema.Builder().addSemanticType(JACAMO + "InitParams").build())
                .addRequiredProperties("artifactClass", ARTIFACT_NAME_PARAM)
                .build()
            ).addSemanticType(JACAMO + "MakeArtifact")
            .build()
        )
        .addAction(
          new ActionAffordance.Builder(
            "joinWorkspace",
            new Form.Builder(thingUri + "join")
              .setMethodName(HttpMethod.POST.name())
              .build()
          ).addSemanticType(JACAMO + "JoinWorkspace")
            .build()
        )
        .addAction(
          new ActionAffordance.Builder(
            "quitWorkspace",
            new Form.Builder(thingUri + "leave")
              .setMethodName(HttpMethod.POST.name())
              .build()
          ).addSemanticType(JACAMO + "QuitWorkspace")
            .build()
        )
        .addAction(
          new ActionAffordance.Builder(
            "focus",
            new Form.Builder(thingUri + "focus")
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
        )
        .addAction(
          new ActionAffordance.Builder(
            "createSubWorkspace",
            new Form.Builder(thingUri)
              .setMethodName(HttpMethod.POST.name())
              .build()
          ).addSemanticType(JACAMO + "CreateSubWorkspace")
            .build()
        );
    addWebSub(td,"Workspace");
    wrapInResourceProfile(td, thingUri, thingUri + "#workspace");
return serializeThingDescription(td);
  }

  @Override
  public String createArtifactRepresentation(final String workspaceName, final String artifactName,
                                             final String semanticType) {
    return createArtifactRepresentation(
      workspaceName,
      artifactName,
      SecurityScheme.getNoSecurityScheme(),
      semanticType,
      new LinkedHashModel(),
      Multimaps.newListMultimap(new HashMap<>(), ArrayList::new)
    );
  }

  @Override
  public String createArtifactRepresentation(final String workspaceName, final String artifactName,
                                             final String semanticType, final Model metadata,
                                             final ListMultimap<String, Object> actionAffordances) {
    return createArtifactRepresentation(
      workspaceName,
      artifactName,
      SecurityScheme.getNoSecurityScheme(),
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
    final ListMultimap<String, ActionAffordance> actionAffordancesMap = Multimaps.newListMultimap(new HashMap<>(),
      ArrayList::new);
    actionAffordances.entries().forEach(entry -> {
      final var actionName = entry.getKey();
      final var action = (ActionAffordance) entry.getValue();
      actionAffordancesMap.put(actionName, action);
    });
    final var thingUri = this.httpConfig.getArtifactUri(workspaceName,artifactName);
    final var td =
      new ThingDescription.Builder(artifactName)
        .addSecurityScheme(securityScheme.getSchemeName(), securityScheme)
        .addSemanticType(HMAS + "Artifact")
        .addSemanticType(semanticType)
        .addThingURI(thingUri + HASH_ARTIFACT)
        .addGraph(metadata);
    actionAffordancesMap.values().forEach(td::addAction);
    addWebSub(td, "Artifact");
    wrapInResourceProfile(td, thingUri, thingUri + HASH_ARTIFACT);
    return serializeThingDescription(td);
  }

  @Override
  public String createBodyRepresentation(
    final String workspaceName,
    final String agentName,
    final Model metadata) {
    return createBodyRepresentation(workspaceName, agentName, SecurityScheme.getNoSecurityScheme(), metadata);
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
        .addSemanticType(HMAS + "Artifact")
        .addSemanticType(JACAMO + "Body")
        .addThingURI(bodyUri + HASH_ARTIFACT)
        .addGraph(metadata);
    addWebSub(td, "Agent");
    wrapInResourceProfile(td, bodyUri, bodyUri + HASH_ARTIFACT);
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
      .write();
  }
}
