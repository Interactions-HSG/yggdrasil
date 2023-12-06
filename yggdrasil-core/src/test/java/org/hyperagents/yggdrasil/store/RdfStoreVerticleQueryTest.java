package org.hyperagents.yggdrasil.store;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.apache.http.HttpStatus;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@ExtendWith(VertxExtension.class)
public class RdfStoreVerticleQueryTest {
  private static final String PLATFORM_URI = "http://localhost:8080/";
  private static final String TEST_WORKSPACE_URI = PLATFORM_URI + "workspaces/test";
  private static final String CONTENTS_EQUAL_MESSAGE = "The contents should be equal";

  private RdfStoreMessagebox messagebox;

  @BeforeEach
  public void setUp(final Vertx vertx, final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    this.messagebox = new RdfStoreMessagebox(vertx.eventBus());
    new HttpNotificationDispatcherMessagebox(vertx.eventBus()).init();
    final var inputWorkspaceRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("test_workspace_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    final var inputSubWorkspaceRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("sub_workspace_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    final var inputArtifactRepresentation =
        Files.readString(
          Path.of(ClassLoader.getSystemResource("c0_counter_artifact_sub_td.ttl").toURI()),
          StandardCharsets.UTF_8
        );
    vertx.deployVerticle(new RdfStoreVerticle())
         .compose(i -> this.messagebox.sendMessage(new RdfStoreMessage.CreateWorkspace(
           "http://localhost:8080/workspaces/",
           "test",
           Optional.empty(),
           inputWorkspaceRepresentation
         )))
         .compose(r -> this.messagebox.sendMessage(new RdfStoreMessage.CreateWorkspace(
           "http://localhost:8080/workspaces/",
           "sub",
           Optional.of(TEST_WORKSPACE_URI),
           inputSubWorkspaceRepresentation
         )))
         .compose(r -> this.messagebox.sendMessage(new RdfStoreMessage.CreateArtifact(
           "http://localhost:8080/workspaces/sub/artifacts/",
           "c0",
           inputArtifactRepresentation
         )))
         .onComplete(ctx.succeedingThenComplete());
  }

  @AfterEach
  public void tearDown(final Vertx vertx, final VertxTestContext ctx) {
    vertx.close(ctx.succeedingThenComplete());
  }

  @Test
  public void testCsvTupleQueryRequest(final VertxTestContext ctx) {
    this.testTupleQueryRequest(List.of(), List.of(), "text/csv")
        .onSuccess(r -> Assertions.assertEquals(
          """
            workspace,artifact\r
            http://localhost:8080/workspaces/sub,c0\r
            http://localhost:8080/workspaces/sub,c0\r
            http://localhost:8080/workspaces/sub,c0\r
            http://localhost:8080/workspaces/sub,c0\r
            http://localhost:8080/workspaces/sub,c0\r
            http://localhost:8080/workspaces/sub,c0\r
            """,
            r.body(),
            CONTENTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testTsvTupleQueryRequest(final VertxTestContext ctx) {
    this.testTupleQueryRequest(List.of(), List.of(), "text/tab-separated-values")
        .onSuccess(r -> Assertions.assertEquals(
          """
            ?workspace\t?artifact
            <http://localhost:8080/workspaces/sub>\tc0
            <http://localhost:8080/workspaces/sub>\tc0
            <http://localhost:8080/workspaces/sub>\tc0
            <http://localhost:8080/workspaces/sub>\tc0
            <http://localhost:8080/workspaces/sub>\tc0
            <http://localhost:8080/workspaces/sub>\tc0
            """,
          r.body(),
          CONTENTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testJsonTupleQueryRequest(final VertxTestContext ctx) {
    this.testTupleQueryRequest(List.of(), List.of(), "application/sparql-results+json")
        .onSuccess(r -> Assertions.assertEquals(
          JsonObject.of(
            "head",
            JsonObject.of(
              "vars",
              JsonArray.of("workspace", "artifact")
            ),
            "results",
            JsonObject.of(
              "bindings",
              JsonArray.of(
                IntStream.range(0, 6)
                         .mapToObj(i -> JsonObject.of(
                           "artifact",
                           JsonObject.of(
                             "type",
                             "literal",
                             "value",
                             "c0"
                           ),
                           "workspace",
                           JsonObject.of(
                             "type",
                             "uri",
                             "value",
                             "http://localhost:8080/workspaces/sub"
                           )
                         ))
                         .toArray()
              )
            )
          ),
          Json.decodeValue(r.body()),
          CONTENTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testXmlTupleQueryRequest(final VertxTestContext ctx) {
    this.testTupleQueryRequest(List.of(), List.of(), "application/sparql-results+xml")
        .onSuccess(r -> Assertions.assertEquals(
          """
            <?xml version='1.0' encoding='UTF-8'?>
            <sparql xmlns='http://www.w3.org/2005/sparql-results#'>
            	<head>
            		<variable name='workspace'/>
            		<variable name='artifact'/>
            	</head>
            	<results>
            		<result>
            			<binding name='artifact'>
            				<literal>c0</literal>
            			</binding>
            			<binding name='workspace'>
            				<uri>http://localhost:8080/workspaces/sub</uri>
            			</binding>
            		</result>
            		<result>
            			<binding name='artifact'>
            				<literal>c0</literal>
            			</binding>
            			<binding name='workspace'>
            				<uri>http://localhost:8080/workspaces/sub</uri>
            			</binding>
            		</result>
            		<result>
            			<binding name='artifact'>
            				<literal>c0</literal>
            			</binding>
            			<binding name='workspace'>
            				<uri>http://localhost:8080/workspaces/sub</uri>
            			</binding>
            		</result>
            		<result>
            			<binding name='artifact'>
            				<literal>c0</literal>
            			</binding>
            			<binding name='workspace'>
            				<uri>http://localhost:8080/workspaces/sub</uri>
            			</binding>
            		</result>
            		<result>
            			<binding name='artifact'>
            				<literal>c0</literal>
            			</binding>
            			<binding name='workspace'>
            				<uri>http://localhost:8080/workspaces/sub</uri>
            			</binding>
            		</result>
            		<result>
            			<binding name='artifact'>
            				<literal>c0</literal>
            			</binding>
            			<binding name='workspace'>
            				<uri>http://localhost:8080/workspaces/sub</uri>
            			</binding>
            		</result>
            	</results>
            </sparql>
            """,
          r.body(),
          CONTENTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testTupleQueryRequestWithDefaultUris(final VertxTestContext ctx) {
    this.testTupleQueryRequest(
            List.of(
              "http://localhost:8080/workspaces/sub",
              "http://localhost:8080/workspaces/sub/artifacts/c0"
            ),
            List.of(),
            "text/csv"
        )
        .onSuccess(r -> Assertions.assertEquals(
            """
              workspace,artifact\r
              http://localhost:8080/workspaces/sub,c0\r
              http://localhost:8080/workspaces/sub,c0\r
              http://localhost:8080/workspaces/sub,c0\r
              http://localhost:8080/workspaces/sub,c0\r
              """,
            r.body(),
            CONTENTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testTupleQueryRequestWithNamedUris(final VertxTestContext ctx) {
    this.testTupleQueryRequest(
            List.of(),
            List.of(
              "http://localhost:8080/workspaces/sub",
              "http://localhost:8080/workspaces/sub/artifacts/c0"
            ),
            "text/csv"
        )
        .onSuccess(r -> Assertions.assertEquals(
            "workspace,artifact\r\n",
            r.body(),
            CONTENTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  private Future<Message<String>> testTupleQueryRequest(
      final List<String> defaultGraphUris,
      final List<String> namedGraphUris,
      final String responseContentType
  ) {
    return this.messagebox
               .sendMessage(new RdfStoreMessage.QueryKnowledgeGraph(
                 """
                   PREFIX td: <https://www.w3.org/2019/wot/td#>
                   PREFIX hmas: <https://purl.org/hmas/core/>
                   PREFIX ex: <http://example.org/>

                   SELECT ?workspace ?artifact
                   WHERE {
                       ?workspace hmas:contains [
                                      a hmas:Artifact, ex:Counter;
                                      td:title ?artifact;
                                  ];
                                  a hmas:Workspace.
                   }
                   """,
                   defaultGraphUris,
                   namedGraphUris,
                   responseContentType
               ));
  }

  @Test
  public void testCsvBooleanQueryRequest(final VertxTestContext ctx) {
    this.testBooleanQueryRequest(List.of(), List.of(), "text/csv")
        .onSuccess(r -> Assertions.assertEquals(
            "true",
            r.body(),
            CONTENTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testTsvBooleanQueryRequest(final VertxTestContext ctx) {
    this.testBooleanQueryRequest(List.of(), List.of(), "text/tab-separated-values")
        .onSuccess(r -> Assertions.assertEquals(
          "true",
          r.body(),
          CONTENTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testJsonBooleanQueryRequest(final VertxTestContext ctx) {
    this.testBooleanQueryRequest(List.of(), List.of(), "application/sparql-results+json")
        .onSuccess(r -> Assertions.assertEquals(
          JsonObject.of(
            "head",
            JsonObject.of(),
            "boolean",
            true
          ),
          Json.decodeValue(r.body()),
          CONTENTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testXmlBooleanQueryRequest(final VertxTestContext ctx) {
    this.testBooleanQueryRequest(List.of(), List.of(), "application/sparql-results+xml")
        .onSuccess(r -> Assertions.assertEquals(
          """
            <?xml version='1.0' encoding='UTF-8'?>
            <sparql xmlns='http://www.w3.org/2005/sparql-results#'>
            	<head>
            	</head>
            	<boolean>true</boolean>
            </sparql>
            """,
          r.body(),
          CONTENTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testBooleanQueryRequestWithDefaultUris(final VertxTestContext ctx) {
    this.testBooleanQueryRequest(List.of("http://localhost:8080/"), List.of(), "text/csv")
        .onSuccess(r -> Assertions.assertEquals(
          "false",
          r.body(),
          CONTENTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testBooleanQueryRequestWithNamedUris(final VertxTestContext ctx) {
    this.testBooleanQueryRequest(List.of(), List.of("http://localhost:8080/"), "text/csv")
        .onSuccess(r -> Assertions.assertEquals(
          "false",
          r.body(),
          CONTENTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  private Future<Message<String>> testBooleanQueryRequest(
      final List<String> defaultGraphUris,
      final List<String> namedGraphUris,
      final String responseContentType
  ) {
    return this.messagebox
               .sendMessage(new RdfStoreMessage.QueryKnowledgeGraph(
                   """
                   PREFIX td: <https://www.w3.org/2019/wot/td#>
                   PREFIX hmas: <https://purl.org/hmas/core/>
                   PREFIX ex: <http://example.org/>

                   ASK WHERE {
                       [] hmas:contains [
                           a hmas:Artifact, ex:Counter;
                           td:title "c0";
                       ];
                       a hmas:Workspace.
                   }
                   """,
                   defaultGraphUris,
                   namedGraphUris,
                   responseContentType
               ));
  }

  @Test
  public void testGraphQueryRequest(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var result =
        Files.readString(Path.of(ClassLoader.getSystemResource("simple_query_result.ttl").toURI()));
    this.testGraphQueryRequest(List.of(), List.of())
        .onSuccess(r -> Assertions.assertEquals(
            result,
            r.body(),
            CONTENTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testGraphQueryRequestWithDefaultUris(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var result =
        Files.readString(Path.of(ClassLoader.getSystemResource("default_query_result.ttl").toURI()));
    this.testGraphQueryRequest(
            List.of(
              "http://localhost:8080/workspaces/sub",
              "http://localhost:8080/workspaces/sub/artifacts/c0"
            ),
            List.of()
        )
        .onSuccess(r -> Assertions.assertEquals(
          result,
          r.body(),
          CONTENTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testGraphQueryRequestWithNamedUris(final VertxTestContext ctx)
      throws URISyntaxException, IOException {
    final var result =
        Files.readString(Path.of(ClassLoader.getSystemResource("named_query_result.ttl").toURI()));
    this.testGraphQueryRequest(
            List.of(),
            List.of(
              "http://localhost:8080/workspaces/sub",
              "http://localhost:8080/workspaces/sub/artifacts/c0"
            )
        )
        .onSuccess(r -> Assertions.assertEquals(
          result,
          r.body(),
          CONTENTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  private Future<Message<String>> testGraphQueryRequest(
      final List<String> defaultGraphUris,
      final List<String> namedGraphUris
  ) {
    return this.messagebox
               .sendMessage(new RdfStoreMessage.QueryKnowledgeGraph(
                   """
                   PREFIX td: <https://www.w3.org/2019/wot/td#>
                   PREFIX hmas: <https://purl.org/hmas/core/>
                   PREFIX ex: <http://example.org/>

                   DESCRIBE ?workspace ?artifact
                   WHERE {
                       ?workspace hmas:contains [
                           a hmas:Artifact, ex:Counter;
                           td:title ?artifact;
                       ];
                       a hmas:Workspace.
                   }
                   """,
                   defaultGraphUris,
                   namedGraphUris,
                   "text/turtle"
               ));
  }

  @Test
  public void testCsvQueryRequestWithUnassignedBinding(final VertxTestContext ctx) {
    this.messagebox
        .sendMessage(new RdfStoreMessage.QueryKnowledgeGraph(
          """
            PREFIX td: <https://www.w3.org/2019/wot/td#>
            PREFIX hmas: <https://purl.org/hmas/core/>
            PREFIX ex: <http://example.org/>

            SELECT ?workspace ?artifact
            WHERE {
                ?workspace hmas:contains [
                               a hmas:Artifact, ex:Counter
                           ];
                           a hmas:Workspace.
            }
            """,
            List.of(),
            List.of(),
            "text/csv"
        ))
        .onSuccess(r -> Assertions.assertEquals(
          """
            workspace,artifact\r
            http://localhost:8080/workspaces/sub,\r
            http://localhost:8080/workspaces/sub,\r
            http://localhost:8080/workspaces/sub,\r
            http://localhost:8080/workspaces/sub,\r
            http://localhost:8080/workspaces/sub,\r
            http://localhost:8080/workspaces/sub,\r
            """,
          r.body(),
          CONTENTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testJsonQueryRequestWithUnassignedBinding(final VertxTestContext ctx) {
    this.messagebox
        .sendMessage(new RdfStoreMessage.QueryKnowledgeGraph(
          """
            PREFIX td: <https://www.w3.org/2019/wot/td#>
            PREFIX hmas: <https://purl.org/hmas/core/>
            PREFIX ex: <http://example.org/>

            SELECT ?workspace ?artifact
            WHERE {
                ?workspace hmas:contains [
                               a hmas:Artifact, ex:Counter
                           ];
                           a hmas:Workspace.
            }
            """,
            List.of(),
            List.of(),
            "application/sparql-results+json"
        ))
        .onSuccess(r -> Assertions.assertEquals(
          JsonObject.of(
            "head",
            JsonObject.of(
              "vars",
              JsonArray.of("workspace", "artifact")
            ),
            "results",
            JsonObject.of(
              "bindings",
              JsonArray.of(
                IntStream.range(0, 6)
                         .mapToObj(i -> JsonObject.of(
                           "workspace",
                           JsonObject.of(
                             "type",
                             "uri",
                             "value",
                             "http://localhost:8080/workspaces/sub"
                           )
                         ))
                         .toArray()
              )
            )
          ),
          Json.decodeValue(r.body()),
          CONTENTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testTsvQueryRequestWithUnassignedBinding(final VertxTestContext ctx) {
    this.messagebox
        .sendMessage(new RdfStoreMessage.QueryKnowledgeGraph(
          """
            PREFIX td: <https://www.w3.org/2019/wot/td#>
            PREFIX hmas: <https://purl.org/hmas/core/>
            PREFIX ex: <http://example.org/>

            SELECT ?workspace ?artifact
            WHERE {
                ?workspace hmas:contains [
                               a hmas:Artifact, ex:Counter
                           ];
                           a hmas:Workspace.
            }
            """,
            List.of(),
            List.of(),
            "text/tab-separated-values"
        ))
        .onSuccess(r -> Assertions.assertEquals(
          """
            ?workspace\t?artifact
            <http://localhost:8080/workspaces/sub>\t
            <http://localhost:8080/workspaces/sub>\t
            <http://localhost:8080/workspaces/sub>\t
            <http://localhost:8080/workspaces/sub>\t
            <http://localhost:8080/workspaces/sub>\t
            <http://localhost:8080/workspaces/sub>\t
            """,
          r.body(),
          CONTENTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testXmlQueryRequestWithUnassignedBinding(final VertxTestContext ctx) {
    this.messagebox
        .sendMessage(new RdfStoreMessage.QueryKnowledgeGraph(
          """
            PREFIX td: <https://www.w3.org/2019/wot/td#>
            PREFIX hmas: <https://purl.org/hmas/core/>
            PREFIX ex: <http://example.org/>

            SELECT ?workspace ?artifact
            WHERE {
                ?workspace hmas:contains [
                               a hmas:Artifact, ex:Counter
                           ];
                           a hmas:Workspace.
            }
            """,
            List.of(),
            List.of(),
            "application/sparql-results+xml"
        ))
        .onSuccess(r -> Assertions.assertEquals(
          """
            <?xml version='1.0' encoding='UTF-8'?>
            <sparql xmlns='http://www.w3.org/2005/sparql-results#'>
            	<head>
            		<variable name='workspace'/>
            		<variable name='artifact'/>
            	</head>
            	<results>
            		<result>
            			<binding name='workspace'>
            				<uri>http://localhost:8080/workspaces/sub</uri>
            			</binding>
            		</result>
            		<result>
            			<binding name='workspace'>
            				<uri>http://localhost:8080/workspaces/sub</uri>
            			</binding>
            		</result>
            		<result>
            			<binding name='workspace'>
            				<uri>http://localhost:8080/workspaces/sub</uri>
            			</binding>
            		</result>
            		<result>
            			<binding name='workspace'>
            				<uri>http://localhost:8080/workspaces/sub</uri>
            			</binding>
            		</result>
            		<result>
            			<binding name='workspace'>
            				<uri>http://localhost:8080/workspaces/sub</uri>
            			</binding>
            		</result>
            		<result>
            			<binding name='workspace'>
            				<uri>http://localhost:8080/workspaces/sub</uri>
            			</binding>
            		</result>
            	</results>
            </sparql>
            """,
          r.body(),
          CONTENTS_EQUAL_MESSAGE
        ))
        .onComplete(ctx.succeedingThenComplete());
  }

  @Test
  public void testQueryRequestWithMalformedQuery(final VertxTestContext ctx) {
    this.messagebox
        .sendMessage(new RdfStoreMessage.QueryKnowledgeGraph(
            """
            PREFIX wot: <https://www.w3.org/2019/wot/td#>
            PREFIX eve: <http://w3id.org/eve#>
            SELECT ?thing ?name
            """,
            List.of(),
            List.of(),
            "application/sparql-results+json"
        ))
        .onFailure(t -> Assertions.assertEquals(
            HttpStatus.SC_BAD_REQUEST,
            ((ReplyException) t).failureCode(),
            "The failure code should be BAD REQUEST"
        ))
        .onComplete(ctx.failingThenComplete());
  }
}
